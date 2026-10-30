# وقت‌یار — Hajar Rajaee Booking

Android **Native** (Kotlin / Jetpack Compose) manager with Persian Jalali month/week/day calendar, local client profiles, personal history and session notes, exact-minute appointments and recurring scheduling (daily / weekly / biweekly / Persian monthly / monthly week+weekday / custom dates). Includes session edits, future-series edits/deletions and overlap checks using local SQLite transactions.

## Test APK
1. **Actions → Build Native Android APK** → latest successful run → Artifact **booking-android-debug-apk**.
2. Unzip and install the APK on the POCO F4.
3. Log in using the username and password supplied privately by the project owner. The source contains only a PBKDF2 password verifier, **not the plaintext password**.
4. The new test APK uses application ID suffix .beta, allowing it to coexist with the old prototype. In **More**, change the password.
5. Create a client first, then an appointment. See Jalali **Calendar**, **Clients**, **Appointments** and **More** tabs.

## Architecture
- android/ — Native Android manager: offline local CRUD via SQLiteOpenHelper, Persian ICU conversion, calendar, clients, notes and schedule recurrence.
- server/ — PHP 8.3 and MySQL private invitation booking API with manual manager approval for customers, intended for Linux shared hosting.
- docs/DEPLOYMENT.md — server deployment instructions.

**No VPS, no client-side installation:** customers would only open private links in their browsers after the server is deployed.

## IMPORTANT: beta limitations
- The local Android database and the PHP server database **do not synchronize** yet. The **Online** page is a separate experimental remote manager and requires real hosting configuration.
- The local database is not fully encrypted, does not yet have reliable backup/restore, notifications or exact alarms. Do not enter actual confidential clinical information in this beta.
- The baked-in password verifier is for evaluation only; embedded authentication alone is not production-grade security.
- Recurrence is materialized as a finite series of up to 104 occurrences and edited per occurrence or remaining future group. Arbitrarily selected dates are available.
- APK is a debug test artifact. Real-world deployment requires security, device, storage, timezone, and acceptance testing, as well as production signing.

The project deliberately does not modify the customer-facing production website.
