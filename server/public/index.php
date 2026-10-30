<?php
declare(strict_types=1);
// Private invitation booking API, designed for PHP 8.3+ shared hosting.
date_default_timezone_set('UTC');
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('Referrer-Policy: no-referrer');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');

final class ApiFailure extends RuntimeException {
    public function __construct(string $message, public readonly int $http = 400) {
        parent::__construct($message);
    }
}
function fail(string $message, int $status = 400): never {
    throw new ApiFailure($message, $status);
}
function send(array $payload, int $code = 200): never {
    http_response_code($code);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR);
    exit;
}
function secret(): string { return bin2hex(random_bytes(32)); }
function stamp(DateTimeImmutable $date): string { return $date->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s'); }
function tehran(): DateTimeZone { return new DateTimeZone('Asia/Tehran'); }
function nowUtc(): DateTimeImmutable { return new DateTimeImmutable('now', new DateTimeZone('UTC')); }
function localStamp(string $utc): string {
    return (new DateTimeImmutable($utc, new DateTimeZone('UTC')))->setTimezone(tehran())->format('Y-m-d H:i');
}
function rateLimit(PDO $db, string $scope, int $max, int $seconds): void {
    $ip = (string)($_SERVER['REMOTE_ADDR'] ?? 'unknown');
    $key = hash('sha256', $scope.'|'.$ip);
    $q = $db->prepare('INSERT INTO rate_limits(key_hash,hits,expires_at) VALUES (?,1,DATE_ADD(UTC_TIMESTAMP(),INTERVAL '.(int)$seconds.' SECOND))
        ON DUPLICATE KEY UPDATE hits=IF(expires_at<=UTC_TIMESTAMP(),1,hits+1),
        expires_at=IF(expires_at<=UTC_TIMESTAMP(),DATE_ADD(UTC_TIMESTAMP(),INTERVAL '.(int)$seconds.' SECOND),expires_at)');
    $q->execute([$key]);
    $q = $db->prepare('SELECT hits FROM rate_limits WHERE key_hash=?');
    $q->execute([$key]);
    if ((int)$q->fetchColumn() > $max) fail('Too many requests. Try later.', 429);
}
function requireAdmin(PDO $db): void {
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
    if (!preg_match('/^Bearer ([0-9a-f]{64})$/', $header, $m)) fail('Unauthorized', 401);
    $q = $db->prepare('SELECT id FROM admins WHERE token_hash=? AND token_expires>UTC_TIMESTAMP()');
    $q->execute([hash('sha256', $m[1])]);
    if (!$q->fetchColumn()) fail('Unauthorized', 401);
}
function activeInvitation(PDO $db, string $token, bool $lock = false): array {
    $q = $db->prepare('SELECT * FROM booking_invitations WHERE token_hash=? AND expires_at>UTC_TIMESTAMP() AND revoked_at IS NULL AND used_at IS NULL'.($lock ? ' FOR UPDATE' : ''));
    $q->execute([hash('sha256', $token)]);
    $invite = $q->fetch(PDO::FETCH_ASSOC);
    if (!$invite) fail('Invitation invalid, used or expired', 404);
    return $invite;
}
function dayAndTime(string $day, string $time): array {
    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/D', $day) ||
        !preg_match('/^\d{2}:\d{2}$/D', $time)) fail('Invalid date or time');
    $start = DateTimeImmutable::createFromFormat('!Y-m-d H:i', $day.' '.$time, tehran());
    if (!$start || $start->format('Y-m-d H:i') !== $day.' '.$time) fail('Invalid date or time');
    return [$start, $start->modify('+60 minutes')];
}
function validCustomerSlot(DateTimeImmutable $start, DateTimeImmutable $end): bool {
    $now = new DateTimeImmutable('now', tehran());
    $today = $now->setTime(0, 0);
    return $start >= $now->modify('+4 hours') &&
        $start < $today->modify('+31 days') &&
        $start->format('H:i') >= '10:00' &&
        $end->format('H:i') <= '21:00' &&
        $start->format('Y-m-d') === $end->format('Y-m-d') &&
        ((int)$start->format('i') % 5) === 0;
}
function lockDay(PDO $db, string $day): void {
    $db->prepare('INSERT IGNORE INTO schedule_locks(day_key) VALUES(?)')->execute([$day]);
    $q = $db->prepare('SELECT day_key FROM schedule_locks WHERE day_key=? FOR UPDATE');
    $q->execute([$day]);
    if (!$q->fetchColumn()) fail('Cannot lock booking day', 500);
}
function busy(PDO $db, DateTimeImmutable $start, DateTimeImmutable $end, ?int $ignoreRequest = null): bool {
    $a = stamp($start); $b = stamp($end);
    $q = $db->prepare("SELECT COUNT(*) FROM appointments WHERE status='approved' AND starts_at<? AND ends_at>?");
    $q->execute([$b,$a]);
    if ((int)$q->fetchColumn() > 0) return true;
    $q = $db->prepare("SELECT COUNT(*) FROM booking_requests WHERE status='pending' AND hold_until>UTC_TIMESTAMP()
       AND starts_at<? AND ends_at>? AND (? IS NULL OR id<>?)");
    $q->execute([$b,$a,$ignoreRequest,$ignoreRequest]);
    return (int)$q->fetchColumn() > 0;
}
function freeSlots(PDO $db, string $day): array {
    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/D', $day)) fail('Invalid day');
    [$base, $_] = dayAndTime($day, '10:00');
    $rangeStart=stamp($base);
    $rangeEnd=stamp($base->modify('+11 hours'));
    $q=$db->prepare("SELECT starts_at,ends_at FROM appointments
      WHERE status='approved' AND ends_at>? AND starts_at<?");
    $q->execute([$rangeStart,$rangeEnd]);
    $periods=$q->fetchAll();
    $q=$db->prepare("SELECT starts_at,ends_at FROM booking_requests
      WHERE status='pending' AND hold_until>UTC_TIMESTAMP() AND ends_at>? AND starts_at<?");
    $q->execute([$rangeStart,$rangeEnd]);
    $periods=array_merge($periods,$q->fetchAll());
    $slots=[];
    for ($minutes=0;$minutes<=600;$minutes+=5) {
        $start=$base->modify('+'.$minutes.' minutes');
        $end=$start->modify('+60 minutes');
        if (!validCustomerSlot($start,$end)) continue;
        $a=stamp($start);$b=stamp($end);
        $taken=false;
        foreach ($periods as $p) {
            if ($p['starts_at']<$b && $p['ends_at']>$a) { $taken=true;break; }
        }
        if (!$taken) $slots[]=$start->format('H:i');
    }
    return $slots;
}
try {
    // For cPanel: store server/app/config.php above server/public, outside document root.
    $conf = require dirname(__DIR__).'/app/config.php';
    $db = new PDO($conf['dsn'], $conf['user'], $conf['pass'], [
        PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES=>false
    ]);
    $method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    $path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
    $path = preg_replace('~^/api/v1~', '', $path, 1);
    if ($path === '') $path = '/';
    if (!in_array($method,['GET','POST'],true)) fail('Method not supported',405);
    if ((int)($_SERVER['CONTENT_LENGTH'] ?? 0) > 16384) fail('Request too large',413);
    $raw = file_get_contents('php://input');
    $body = $raw !== '' ? json_decode($raw,true) : [];
    if (!is_array($body)) fail('Invalid JSON');

    if ($path === '/auth/login' && $method === 'POST') {
        rateLimit($db,'admin-login',10,900);
        $name = (string)($body['username'] ?? '');
        $pwd = (string)($body['password'] ?? '');
        $q = $db->prepare('SELECT * FROM admins WHERE username=?');
        $q->execute([$name]);
        $user = $q->fetch();
        if (!$user || !password_verify($pwd, $user['password_hash'])) fail('Invalid credentials',401);
        $token = secret();
        $db->prepare('UPDATE admins SET token_hash=?,token_expires=DATE_ADD(UTC_TIMESTAMP(),INTERVAL 7 DAY) WHERE id=?')
           ->execute([hash('sha256',$token),$user['id']]);
        send(['token'=>$token]);
    }

    if (preg_match('~^/invitations/([0-9a-f]{64})$~D', $path, $m) && $method === 'GET') {
        rateLimit($db,'invitation',100,60);
        $inv = activeInvitation($db, $m[1]);
        send(['name'=>$inv['name'], 'expires_at'=>$inv['expires_at']]);
    }
    if (preg_match('~^/invitations/([0-9a-f]{64})/availability$~D', $path, $m) && $method === 'GET') {
        rateLimit($db,'availability',35,60);
        activeInvitation($db, $m[1]);
        send(['slots'=>freeSlots($db,(string)($_GET['day'] ?? ''))]);
    }
    if (preg_match('~^/invitations/([0-9a-f]{64})/requests$~D', $path, $m) && $method === 'POST') {
        rateLimit($db,'book',10,900);
        $day = (string)($body['day'] ?? '');
        $time = (string)($body['time'] ?? '');
        [$start,$end] = dayAndTime($day,$time);
        if (!validCustomerSlot($start,$end)) fail('Selected time is not available',409);
        $db->beginTransaction();
        try {
            lockDay($db,$day);
            $inv = activeInvitation($db,$m[1],true);
            if (busy($db,$start,$end)) fail('Time has just been reserved. Select another.',409);
            $tracking = secret();
            $hold = nowUtc()->modify('+12 hours');
            $limit = $start->modify('-2 hours')->setTimezone(new DateTimeZone('UTC'));
            if ($limit < $hold) $hold = $limit;
            $q = $db->prepare('INSERT INTO booking_requests
             (invitation_id,name,phone,starts_at,ends_at,hold_until,tracking_hash)
             VALUES(?,?,?,?,?,?,?)');
            $q->execute([$inv['id'],$inv['name'],$inv['phone'],stamp($start),stamp($end),stamp($hold),hash('sha256',$tracking)]);
            $db->prepare('UPDATE booking_invitations SET used_at=UTC_TIMESTAMP() WHERE id=?')->execute([$inv['id']]);
            $db->commit();
            send(['status'=>'pending','tracking_token'=>$tracking],201);
        } catch(Throwable $e) { if ($db->inTransaction()) $db->rollBack(); throw $e; }
    }
    if (preg_match('~^/booking-status/([0-9a-f]{64})$~D', $path, $m) && $method === 'GET') {
        rateLimit($db,'status',100,60);
        $q = $db->prepare('SELECT status,starts_at,hold_until FROM booking_requests WHERE tracking_hash=?');
        $q->execute([hash('sha256',$m[1])]);
        $data = $q->fetch();
        if (!$data) fail('Not found',404);
        if ($data['status']==='pending' && $data['hold_until']<=gmdate('Y-m-d H:i:s')) $data['status']='expired';
        send(['status'=>$data['status'],'starts_at'=>localStamp($data['starts_at'])]);
    }

    if (str_starts_with($path,'/admin/')) requireAdmin($db);
    if ($path === '/admin/sync/snapshot' && $method === 'GET') {
        require dirname(__DIR__).'/app/sync.php';
        snapshot($db);
    }
    if ($path === '/admin/sync/mutate' && $method === 'POST') {
        require dirname(__DIR__).'/app/sync.php';
        rateLimit($db,'sync-write',180,60);
        syncMutation($db,$body);
    }
    if ($path === '/admin/overview' && $method === 'GET') {
        $req = $db->query("SELECT id,name,starts_at,
          IF(status='pending' AND hold_until<=UTC_TIMESTAMP(),'expired',status) AS status
          FROM booking_requests ORDER BY created_at DESC LIMIT 100")->fetchAll();
        $appointments = $db->query("SELECT id,name,starts_at,status FROM appointments
          WHERE status='approved' AND starts_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 DAY)
          ORDER BY starts_at ASC LIMIT 100")->fetchAll();
        foreach ($req as &$r) $r['starts_at']=localStamp($r['starts_at']);
        unset($r);
        foreach ($appointments as &$a) $a['starts_at']=localStamp($a['starts_at']);
        unset($a);
        send(['requests'=>$req,'appointments'=>$appointments]);
    }
    if ($path === '/admin/invitations' && $method === 'POST') {
        $name = trim((string)($body['name'] ?? ''));
        if ($name === '' || strlen($name)>500) fail('Invalid name');
        $phone = trim((string)($body['phone'] ?? ''));
        if (strlen($phone)>32) fail('Invalid phone');
        $token = secret();
        $q = $db->prepare('INSERT INTO booking_invitations(name,phone,token_hash,expires_at)
              VALUES(?,?,?,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 7 DAY))');
        $q->execute([$name,$phone,hash('sha256',$token)]);
        send(['url'=>rtrim($conf['base_url'],'/').'/r/'.$token],201);
    }
    if (preg_match('~^/admin/requests/(\d+)/(approve|reject)$~D', $path, $m) && $method === 'POST') {
        $id=(int)$m[1];
        $q=$db->prepare('SELECT * FROM booking_requests WHERE id=?');
        $q->execute([$id]); $initial=$q->fetch();
        if (!$initial) fail('Not found',404);
        $day=(new DateTimeImmutable($initial['starts_at'],new DateTimeZone('UTC')))->setTimezone(tehran())->format('Y-m-d');
        $db->beginTransaction();
        try {
            lockDay($db,$day);
            $q=$db->prepare('SELECT * FROM booking_requests WHERE id=? FOR UPDATE');
            $q->execute([$id]); $row=$q->fetch();
            if (!$row || $row['status']!=='pending' || $row['hold_until']<=gmdate('Y-m-d H:i:s')) {
                fail('Request is no longer pending',409);
            }
            if ($m[2]==='approve') {
                $start=new DateTimeImmutable($row['starts_at'],new DateTimeZone('UTC'));
                $end=new DateTimeImmutable($row['ends_at'],new DateTimeZone('UTC'));
                if (busy($db,$start,$end,$id)) fail('Time is no longer available',409);
                $db->prepare('INSERT INTO appointments(request_id,name,phone,starts_at,ends_at)
                     VALUES(?,?,?,?,?)')->execute([$id,$row['name'],$row['phone'],$row['starts_at'],$row['ends_at']]);
            }
            $status=$m[2]==='approve'?'approved':'rejected';
            $db->prepare('UPDATE booking_requests SET status=? WHERE id=?')->execute([$status,$id]);
            $db->commit();
            send(['status'=>$status]);
        } catch(Throwable $e) { if ($db->inTransaction()) $db->rollBack(); throw $e; }
    }
    if ($path === '/admin/appointments' && $method === 'POST') {
        $name=trim((string)($body['name'] ?? ''));
        if ($name==='' || strlen($name)>500) fail('Invalid name');
        $day=(string)($body['day'] ?? '');
        $time=(string)($body['time'] ?? '');
        [$start,$end]=dayAndTime($day,$time);
        if ($start < new DateTimeImmutable('now',tehran()) || $end->format('Y-m-d')!==$day) {
            fail('Invalid appointment time');
        }
        $db->beginTransaction();
        try {
            lockDay($db,$day);
            if (busy($db,$start,$end)) fail('Time overlaps another booking',409);
            $db->prepare('INSERT INTO appointments(name,starts_at,ends_at) VALUES(?,?,?)')
                ->execute([$name,stamp($start),stamp($end)]);
            $id=(int)$db->lastInsertId();
            $db->commit();
            send(['id'=>$id,'status'=>'approved'],201);
        } catch(Throwable $e) { if ($db->inTransaction()) $db->rollBack(); throw $e; }
    }
    fail('Endpoint not found',404);
} catch(ApiFailure $e) {
    send(['error'=>$e->getMessage()],$e->http);
} catch(Throwable $e) {
    error_log('Booking API failure: '.$e->getMessage());
    send(['error'=>'Internal server error'],500);
}
