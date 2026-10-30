<?php
// Run in PHP CLI only: php server/tools/hash-password.php
// Do not expose this file through web root.
if (PHP_SAPI !== 'cli') {http_response_code(403); exit;}
fwrite(STDERR, "New server admin password (input visible): ");
$pass = trim((string)fgets(STDIN));
if(strlen($pass)<12){fwrite(STDERR,"Password too short.\n");exit(1);}
echo password_hash($pass,PASSWORD_DEFAULT).PHP_EOL;
