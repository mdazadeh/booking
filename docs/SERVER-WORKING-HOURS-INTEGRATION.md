# Work-hours synchronization — source-only staged change (not deployed)

Six Android beta16 functional tests on POCO F4 were reported successful by the user on October 31, 2026.

## What was added
- `server/app/work_hours.php` manages a seven-day schedule (Saturday through Friday), opening and closing minutes and a break window.
- `server/sql/migration_work_hours.sql` adds **one new table only**; no alteration or deletion of existing appointments or accounts.
- `GET /api/v1/admin/work-hours` reads server policy and version; bearer authentication required.
- `POST /api/v1/admin/work-hours` publishes all seven days using `expected_version` for conflict protection; bearer authentication required.
- Customer availability and reservation submission both enforce the published schedule; the reservation POST rechecks it after locking the date and policy.
- Pending reservation approvals reject newly disallowed windows; already approved appointments are never automatically deleted.

## Backward compatibility / rollout boundary
**Not deployed.** No changes were made to the user's live host or MySQL.
Before migration, GET reflects the legacy policy and POST returns 503; customer behavior remains 10:00-21:00 every day.
After applying the migration, `published=0` still leaves the legacy 10:00–21:00 behavior unchanged.
**Only an explicit administrator POST** switches customer availability to the new weekly policy.
If an admin publishes a new schedule after an invitation was sent, open customer availability will update, and previously submitted requests may need rebooking.
After server PHP update and migration, Android needs a separate screen/button to compare and **explicitly publish** local work hours. Beta16 has local settings only and cannot publish automatically.

## Example JSON body (admin-only)
```json
{"expected_version":1,"days":[
{"enabled":true,"open":540,"close":1140,"break_start":780,"break_end":840},
{"enabled":true,"open":540,"close":1140,"break_start":780,"break_end":840},
{"enabled":true,"open":540,"close":1140,"break_start":780,"break_end":840},
{"enabled":true,"open":540,"close":1140,"break_start":780,"break_end":840},
{"enabled":true,"open":540,"close":1140,"break_start":780,"break_end":840},
{"enabled":true,"open":540,"close":1140,"break_start":780,"break_end":840},
{"enabled":false,"open":540,"close":1140,"break_start":780,"break_end":840}
]}
```

## Required verification before deployment
1. Take MySQL and file backups, and keep a verified rollback archive.
2. In a staging database, apply **only** the new migration SQL.
3. Validate PHP 8.3 syntax and API authentication, authorization, time zone and weekday mapping.
4. Assert old GET available slots unchanged until published.
5. Publish a test schedule and verify: disabled Friday, break rejection, 5-minute increments, midnight rollover, pending hold conflict, approval, and previously approved records retained.
6. Verify Android/server version conflict responses and restore from backup if errors.
7. Only then deploy to production through DirectAdmin/phpMyAdmin with explicit authorization.

Customer pages render slots returned by the server; no public page deployment is required for schedule enforcement.
