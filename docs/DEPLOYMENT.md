# Deployment guide — hajarrajaee.ir

## Requirements
Linux shared hosting with PHP 8.3+, PDO_MYSQL, MySQL/MariaDB InnoDB, HTTPS and Apache mod_rewrite. No VPS. Customers open private links in their phone browser and install nothing.

## Install backend safely
1. Provision a NEW MySQL database/user via your hosting panel.
2. Import server/sql/schema.sql using phpMyAdmin. Never import it into an existing database without a full backup.
3. Copy server/app/config.example.php to server/app/config.php and set database credentials and HTTPS base URL.
4. Keep server/app/ outside the public web root. Point document root at server/public/; the relative config include in server/public/index.php assumes this directory layout. If the host requires public_html, move private configuration outside public_html and adjust the include path.
5. Enable HTTPS and Apache rewrites; verify endpoints.
6. Generate admin password hash from PHP CLI with password_hash or use a one-time secure hosting script removed immediately afterward. Insert username/password_hash into admins using phpMyAdmin. Never use a default password.
7. Test login, private invitation, availability, request, approve/reject and tracking on a test system.
8. Arrange database backups and test restoration before holding client information.

## GitHub APK
Actions > Build Native Android APK > Run workflow. Download artifact booking-android-debug-apk after success. This is a DEBUG build.

## Known limitations
No automatic server deployment, SMS, push notifications, exact Android alarms, full Persian native edit calendar, recurrence, client notes, Room offline cache or production release signing. Manual appointment date is entered as ISO Gregorian in this test version, but stored with Tehran time zone and displayed as Jalali. This MVP is not for real sensitive medical information without additional security QA.

## Security notes
Invitation and tracking tokens are random and database-hashed. Sharing a token grants access to the link's capability, not proof of customer's identity. Use HTTPS, hosting firewall/rate limits, access log hygiene, routine backups and strong admin password. API login token is presently stored in app-private preferences; plan further secure token storage before production.
