# وقت‌یار — VaghYar Booking (v1.3 Beta)

Native Android manager written in Kotlin + Jetpack Compose, with customer appointments booked through private browser links hosted on Linux shared hosting (PHP + MySQL/InnoDB). **Customers do not install anything and no VPS is needed.**

## Functional beta components

- Persian (Jalali) month/week/day calendar, Tehran timezone and minute-precision scheduling.
- Local client profiles, history, per-session notes; recurring appointment series with edit/delete.
- Appointment notifications using AlarmManager (exact where permission granted, otherwise best-effort inexact), reboot re-scheduling and configurable lead time.
- Explainable local recommendations of free slots; requires server verification when synced.
- Android Keystore AES-GCM encryption of local personal text fields (not full database encryption).
- Portable AES-256-GCM encrypted backup with independent passphrase, restore via Android file picker.
- **Schedule-only sync**: push local appointments and receive customer-approved server appointments using server-side day locks, idempotency keys and conflict reporting. Notes and case histories remain local and are **not** synced.
- WorkManager best-effort periodic sync; 15-minute cadence is not a real-time guarantee.
- Emerald glass-inspired launcher icon, gradient header and translucent cards.

## GitHub downloads

**Actions → Build Native Android APK**: download the artifact named `booking-android-debug-apk` from the latest **successful** Android run. Extract `app-debug.apk`. This is a debug APK, not a production-signed release.

**Actions → Package Shared Hosting**: download `booking-shared-hosting-zip` and extract the nested hosting ZIP. It contains server app/public/sql and the installer guide, without real credentials.

## First-run use

1. Install the current beta APK; enter the local test credentials supplied separately by the project owner.
2. Change local password from More.
3. Create a client and a session; test recurrence, notes, smart slots and reminders.
4. Export and verify a password-protected backup before using restore.
5. Provision hosting following [HOSTING-SETUP.md](docs/HOSTING-SETUP.md). Configure a dedicated document root and MySQL database.
6. Create the **server** admin account with PHP password_hash on the server, then connect from Android More → Server sync.
7. Use the separate legacy online panel for private invitation links and request approvals until it is integrated into the new native navigation.

## Safety and scope

**Development beta only.** No unattended server deployment or POCO F4 physical verification has been performed. Debug CI signatures can change; the v1.3 beta uses a distinct package suffix to coexist with older debug APKs, but therefore does **not** automatically import prior beta app data. Do not uninstall prior app versions if they contain records without a verified backup.

Do not store real confidential clinical notes until device, security and restore QA are completed. Scheduling metadata is not fully encrypted. Automatic sync requires a reachable, properly configured HTTPS backend; offline entries are provisional until server accepts them. SMS intentionally deferred.

- [Shared-hosting instructions](docs/HOSTING-SETUP.md)
- [POCO F4 device QA checklist](docs/POCO-F4-QA.md)
- [PHP hosting setup / migrations](server/sql/)
