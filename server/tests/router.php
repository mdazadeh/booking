<?php
// Routing adapter for PHP built-in server, TEST ONLY (Apache uses .htaccess in production).
$path=parse_url($_SERVER['REQUEST_URI'],PHP_URL_PATH);
if(str_starts_with($path,'/api/v1/')){require dirname(__DIR__).'/public/index.php';return true;}
if(preg_match('~^/r/[a-f0-9]{64}/?$~',$path)){require dirname(__DIR__).'/public/r/index.php';return true;}
if($path==='/status/'||$path==='/status'){require dirname(__DIR__).'/public/status/index.php';return true;}
return false;
