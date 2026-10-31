<?php
declare(strict_types=1);
date_default_timezone_set('UTC');
function tehran():DateTimeZone { return new DateTimeZone('Asia/Tehran'); }
function fail(string $message,int $status=400):never { throw new InvalidArgumentException($message,$status); }
require __DIR__.'/../app/work_hours.php';
function check(bool $value,string $message):void{
    if(!$value)throw new RuntimeException('Failed: '.$message);
}
function local(string $text):DateTimeImmutable{
    return new DateTimeImmutable($text,new DateTimeZone('Asia/Tehran'));
}
$work=[];
for($i=0;$i<7;$i++)$work[]=[
    'enabled'=>$i!==6,'open'=>540,'close'=>1140,'break_start'=>780,'break_end'=>840
];
$days=validateWorkDays($work);
$policy=['days'=>$days,'published'=>true];
check(scheduleAllows(local('2026-10-31 09:00'),local('2026-10-31 10:00'),$policy),'Saturday opening');
check(!scheduleAllows(local('2026-11-06 09:00'),local('2026-11-06 10:00'),$policy),'Friday closed');
check(!scheduleAllows(local('2026-10-31 13:30'),local('2026-10-31 14:30'),$policy),'break overlap');
check(scheduleAllows(local('2026-10-31 12:00'),local('2026-10-31 13:00'),$policy),'before break');
check(scheduleAllows(local('2026-10-31 14:00'),local('2026-10-31 15:00'),$policy),'after break');
check(!scheduleAllows(local('2026-10-31 18:30'),local('2026-10-31 19:30'),$policy),'closing cutoff');
check(!scheduleAllows(local('2026-10-31 23:30'),local('2026-11-01 00:30'),$policy),'midnight crossover');
$legacy=['days'=>legacyWorkDays()];
check(scheduleAllows(local('2026-11-06 20:00'),local('2026-11-06 21:00'),$legacy),'old Friday legacy open');
check(!scheduleAllows(local('2026-11-06 09:00'),local('2026-11-06 10:00'),$legacy),'old day starts at 10');
$failed=false;
try{validateWorkDays(array_slice($work,0,6));}catch(InvalidArgumentException){$failed=true;}
check($failed,'invalid day count rejected');
$failed=false;
$bad=$work;$bad[0]['close']=500;
try{validateWorkDays($bad);}catch(InvalidArgumentException){$failed=true;}
check($failed,'invalid interval rejected');
echo "Working-hours unit checks passed\n";
