<?php
declare(strict_types=1);

/**
 * Appointment schedule policy; no changes to existing appointment or request rows.
 * Unpublished policy exactly preserves the old public 10:00–21:00 daily window.
 */
function legacyWorkDays():array {
    return array_fill(0,7,[
        'enabled'=>true,'open'=>600,'close'=>1260,'break_start'=>0,'break_end'=>0
    ]);
}
function schedulePolicy(PDO $db,bool $lock=false):array {
    try {
        $q=$db->query('SELECT version,published,days_json FROM booking_schedule_policy WHERE id=1'.($lock?' FOR UPDATE':''));
        $row=$q->fetch(PDO::FETCH_ASSOC);
    }catch(PDOException $e){
        if((string)$e->getCode()==='42S02'){
            return ['version'=>0,'published'=>false,'migrated'=>false,'days'=>legacyWorkDays()];
        }
        throw $e;
    }
    if(!$row)return ['version'=>0,'published'=>false,'migrated'=>false,'days'=>legacyWorkDays()];
    $active=(int)$row['published']===1;
    if(!$active)return ['version'=>(int)$row['version'],'published'=>false,
        'migrated'=>true,'days'=>legacyWorkDays()];
    $days=json_decode((string)$row['days_json'],true,16,JSON_THROW_ON_ERROR);
    $days=validateWorkDays($days);
    return ['version'=>(int)$row['version'],'published'=>true,'migrated'=>true,'days'=>$days];
}
function validateWorkDays(mixed $rows):array {
    if(!is_array($rows)||!array_is_list($rows)||count($rows)!==7)
        fail('Exactly seven work days are required.');
    $result=[];
    foreach($rows as $day){
        if(!is_array($day) || !isset($day['enabled'],$day['open'],$day['close'],$day['break_start'],$day['break_end']))
            fail('Incomplete work day settings.');
        if(!is_bool($day['enabled']))fail('Working day flag must be boolean.');
        foreach(['open','close','break_start','break_end'] as $field){
            if(!is_int($day[$field]))fail('Working hours must be minute integers.');
        }
        $open=$day['open'];$close=$day['close'];$a=$day['break_start'];$b=$day['break_end'];
        if($open<0||$open>=1440||$close<=0||$close>1440||$open>=$close||
            $a<0||$a>1440||$b<0||$b>1440||
            ($b>$a && ($a<$open || $b>$close)))fail('Invalid working hours.');
        $result[]=['enabled'=>$day['enabled'],'open'=>$open,'close'=>$close,
            'break_start'=>$a,'break_end'=>$b];
    }
    return $result;
}
function scheduleAllows(DateTimeImmutable $start,DateTimeImmutable $end,array $policy):bool {
    $s=$start->setTimezone(tehran());
    $e=$end->setTimezone(tehran());
    if($s->format('Y-m-d')!==$e->format('Y-m-d'))return false;
    $dayIndex=((int)$s->format('w')+1)%7; // Saturday=0 through Friday=6
    $window=$policy['days'][$dayIndex];
    if(!$window['enabled'])return false;
    $a=(int)$s->format('H')*60+(int)$s->format('i');
    $b=(int)$e->format('H')*60+(int)$e->format('i');
    if($a<$window['open']||$b>$window['close']||$b<=$a)return false;
    return !($window['break_end']>$window['break_start'] &&
        $a<$window['break_end'] && $b>$window['break_start']);
}
function publishSchedulePolicy(PDO $db,array $body):array {
    $days=validateWorkDays($body['days']??null);
    $expected=$body['expected_version']??null;
    if(!is_int($expected)||$expected<1)fail('Expected schedule version is required.');
    $db->beginTransaction();
    try{
        $current=schedulePolicy($db,true);
        if(!$current['migrated'])fail('Install the schedule migration first.',503);
        if($current['version']!==$expected)fail('Schedule updated elsewhere. Refresh before publishing.',409);
        $json=json_encode($days,JSON_THROW_ON_ERROR|JSON_UNESCAPED_UNICODE);
        $q=$db->prepare('UPDATE booking_schedule_policy SET published=1,version=version+1,days_json=?,updated_at=UTC_TIMESTAMP() WHERE id=1 AND version=?');
        $q->execute([$json,$expected]);
        if($q->rowCount()!==1)fail('Schedule version conflict.',409);
        $db->commit();
        return ['version'=>$expected+1,'published'=>true,'migrated'=>true,'days'=>$days];
    }catch(Throwable $e){if($db->inTransaction())$db->rollBack();throw $e;}
}
