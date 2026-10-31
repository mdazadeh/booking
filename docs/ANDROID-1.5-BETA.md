# VaghYar v1.5 beta16 - implementation and migration

## Implemented in Android
- Minute-scaled day timeline with click-to-book free rows and correctly positioned appointment blocks.
- Month and week date selection preview day appointments without automatically forcing the new-booking sheet.
- Booking edit: change Persian date, start time and duration; confirm conflict before write.
- Working hours and break windows for Saturday-Friday managed from the More screen. Local editor enforces chosen windows, including repeated sessions.
- Local-only attendance: scheduled, arrived, completed, no-show (stored in new SQLite side table, never written to the existing server ENUM).
- Atomic local client + session write, so an unavailable booking doesn't leave a partial new client.
- Requests, private invite links and approval use the same encrypted SyncClient session as synchronization.
- Persian UI/Vazirmatn and bidirectional phone input from 1.4 retained.

## Caution: server is deliberately unchanged
The currently deployed PHP backend does **not** enforce the Android-configured working hours or break windows. Customer availability may still show slots outside the local working hours. **Do not treat local schedule settings as synchronized server policy**. A separate server rollout and end-to-end QA are required before using hours in real customer booking.
Attendance statuses are local-only. The server remains 'approved' or 'cancelled' to preserve compatibility.

## Data preservation
- Local SQLite file name remains `booking_local_1_2.db`. SQLite migrates from version 2 to 3 with an additive `session_progress` table. No client or session records are deleted by upgrade.
- Portable encrypted backup now includes attendance; older backups lacking `progress` restore as `scheduled`.
- GitHub Actions debug APK uses ephemeral signing keys. New `.beta16` app is installed **side by side** with beta15 to avoid update-signature conflicts. It does not share beta15's private storage.
- **Before installing beta16, export a password-protected backup (`.vybk`) from the old app.** Import using More > Encrypted Backup in beta16, verify records, and retain the old app until confirmation.
- Do not connect both copies to the same server concurrently until sync reconciliation is tested.
- Don't use sensitive real data in the beta release. Change the test-only local login password in the app and keep server credentials private.

## QA checklist
1. Open calendar; month selection previews the selected day, not forced form.
2. Switch to day; verify grid time positions and duration-proportional event blocks.
3. Tap free slot -> quick booking with prefilled day/time.
4. Save a new client + appointment, reject a conflict; confirm no orphan client after failure.
5. Tap event -> change Jalali date/time; verify overlap rejection.
6. Set working hours / break and ensure invalid appointments are rejected locally, including repeats.
7. Set arrived/completed/no-show and confirm visual badge and encrypted backup round-trip.
8. Connect once from Requests or More; approve test private booking and synchronize to calendar.
9. Reopen after process exit and restart phone; verify data and notes retained.
10. Verify no duplicate remote sessions, correct Iran time zone and privacy.
11. **Live server hours are not yet enforced** and APK is not a release-signed production build.
