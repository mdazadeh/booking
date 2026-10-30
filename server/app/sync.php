<?php
declare(strict_types=1);
/**
 * Server-authoritative schedule sync used by the Native Android app.
 * Included AFTER requireAdmin() in index.php.
 * Appointments are stored as UTC strings; every mutation serializes on day locks.
 */
function parseUtcFromMillis(mixed $millis): DateTimeImmutable {
    if (!is_int($millis) && !ctype_digit((string)$millis)) fail('Invalid start timestamp');
    $ms=(int)$millis;
    if($ms<946684800000 || $ms>4102444800000)fail('Date out of range');
    return (new DateTimeImmutable('@'.intdiv($ms,1000)))->setTimezone(new DateTimeZone('UTC'));
}
function lockDays(PDO $db, array $days): void {
    $days=array_values(array_unique($days));
    sort($days,SORT_STRING);
    foreach($days as $day) lockDay($db,$day);
}
function localDay(DateTimeImmutable $date):string {
    return $date->setTimezone(tehran())->format('Y-m-d');
}
function snapshot(PDO $db):never {
    // Assign persistent sync keys to legacy web bookings before reading the snapshot.
    $legacy=$db->query("SELECT id FROM appointments WHERE sync_key IS NULL")->fetchAll(PDO::FETCH_COLUMN);
    $assign=$db->prepare("UPDATE appointments SET sync_key=? WHERE id=? AND sync_key IS NULL");
    foreach($legacy as $legacyId) $assign->execute([bin2hex(random_bytes(16)),(int)$legacyId]);
    $q=$db->query("SELECT id,sync_key,sync_version,name,phone,starts_at,ends_at,status,request_id
       FROM appointments WHERE starts_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 180 DAY)
       ORDER BY starts_at ASC LIMIT 5000");
    $entries=[];
    foreach($q as $r){
        $entries[]=[
            'remote_id'=>(int)$r['id'],'sync_key'=>$r['sync_key'],
            'version'=>(int)$r['sync_version'],'name'=>$r['name'],
            'phone'=>$r['phone']??'','starts_ms'=>strtotime($r['starts_at'].' UTC')*1000,
            'minutes'=>(int)((strtotime($r['ends_at'].' UTC')-strtotime($r['starts_at'].' UTC'))/60),
            'status'=>$r['status'],'from_customer'=>$r['request_id']!==null
        ];
    }
    send(['appointments'=>$entries,'count'=>count($entries),'truncated'=>count($entries)===5000]);
}
function syncMutation(PDO $db,array $body):never {
    $action=(string)($body['action']??'');
    if(!in_array($action,['create','update','cancel'],true))fail('Unknown sync action');
    $key=(string)($body['sync_key']??'');
    if(!preg_match('/^[a-f0-9-]{16,64}$/D',$key))fail('Invalid sync identifier');
    $id=(int)($body['remote_id']??0);
    $expected=(int)($body['expected_version']??0);
    if($action!=='create' && ($id<=0||$expected<=0))fail('Missing version');
    $name=trim((string)($body['name']??''));
    $phone=trim((string)($body['phone']??''));
    $duration=(int)($body['minutes']??60);
    if($action!=='cancel' && ($name===''||mb_strlen($name)>190||strlen($phone)>32||$duration<5||$duration>480))
        fail('Invalid appointment fields');
    $newStart=($action==='cancel')?null:parseUtcFromMillis($body['starts_ms']??null);
    $newEnd=$newStart? $newStart->modify('+'.$duration.' minutes'):null;
    $old=null;
    if($id>0){
        $q=$db->prepare('SELECT * FROM appointments WHERE id=? AND sync_key=?');
        $q->execute([$id,$key]);$old=$q->fetch();
        if(!$old)fail('Appointment not found',409);
    }
    $days=[];
    if($newStart){$days[]=localDay($newStart);$days[]=localDay($newEnd->modify('-1 second'));}
    if($old){
        $prior=new DateTimeImmutable($old['starts_at'],new DateTimeZone('UTC'));
        $oldEnd=new DateTimeImmutable($old['ends_at'],new DateTimeZone('UTC'));
        $days[]=localDay($prior);$days[]=localDay($oldEnd->modify('-1 second'));
    }
    $db->beginTransaction();
    try{
        lockDays($db,$days);
        if($action==='create'){
            $q=$db->prepare('SELECT id,sync_version FROM appointments WHERE sync_key=? FOR UPDATE');
            $q->execute([$key]);$exists=$q->fetch();
            if($exists) {
                $db->commit();send(['remote_id'=>(int)$exists['id'],'version'=>(int)$exists['sync_version']]);
            }
            if(busy($db,$newStart,$newEnd))fail('Slot not available on central server',409);
            $q=$db->prepare('INSERT INTO appointments(sync_key,name,phone,starts_at,ends_at,status) VALUES(?,?,?,?,?,?)');
            $q->execute([$key,$name,$phone,stamp($newStart),stamp($newEnd),'approved']);
            $answer=['remote_id'=>(int)$db->lastInsertId(),'version'=>1];
        }else{
            $q=$db->prepare('SELECT * FROM appointments WHERE id=? AND sync_key=? FOR UPDATE');
            $q->execute([$id,$key]);$current=$q->fetch();
            if(!$current || (int)$current['sync_version']!==$expected)fail('Remote appointment changed',409);
            if($action==='cancel'){
                if($current['status']==='cancelled')fail('Already cancelled',409);
                $q=$db->prepare("UPDATE appointments SET status='cancelled',sync_version=sync_version+1 WHERE id=?");
                $q->execute([$id]);
                if($current['request_id']!==null) {
                    $db->prepare("UPDATE booking_requests SET status='cancelled' WHERE id=?")
                       ->execute([$current['request_id']]);
                }
            }else{
                // Exclude self when checking overlaps, but include active customer holds.
                $q=$db->prepare("SELECT COUNT(*) FROM appointments WHERE status='approved'
                  AND id<>? AND starts_at<? AND ends_at>?");
                $q->execute([$id,stamp($newEnd),stamp($newStart)]);
                if((int)$q->fetchColumn()>0)fail('Time conflicts with existing appointment',409);
                $q=$db->prepare("SELECT COUNT(*) FROM booking_requests WHERE status='pending'
                    AND hold_until>UTC_TIMESTAMP() AND starts_at<? AND ends_at>?");
                $q->execute([stamp($newEnd),stamp($newStart)]);
                if((int)$q->fetchColumn()>0)fail('Time held by pending booking',409);
                $q=$db->prepare("UPDATE appointments SET name=?,phone=?,starts_at=?,ends_at=?,
                    sync_version=sync_version+1 WHERE id=? AND status='approved'");
                $q->execute([$name,$phone,stamp($newStart),stamp($newEnd),$id]);
                if($q->rowCount()!==1)fail('Appointment not active',409);
                if($current['request_id']!==null){
                    $db->prepare("UPDATE booking_requests SET starts_at=?,ends_at=? WHERE id=? AND status='approved'")
                       ->execute([stamp($newStart),stamp($newEnd),$current['request_id']]);
                }
            }
            $answer=['remote_id'=>$id,'version'=>$expected+1];
        }
        $db->commit();send($answer);
    }catch(Throwable $e){if($db->inTransaction())$db->rollBack();throw $e;}
}
