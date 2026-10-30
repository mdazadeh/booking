# VaghYar | وقت‌یار — Booking v1.1 development MVP

A native Android manager (Kotlin + Jetpack Compose), private customer booking links opened only in a browser, and a PHP/MySQL booking API designed for Linux shared hosting. **No customer app installation and no VPS.**

## Structure

- android/ — Android Studio project; GitHub Actions builds a debug APK.
- server/public/ — private booking site, tracking page and REST API.
- server/sql/schema.sql — initial InnoDB database schema.
- server/app/config.example.php — template; live secrets are never committed.
- docs/DEPLOYMENT.md — setup and security notes.

## Download Android APK through GitHub

1. Select Actions > Build Native Android APK.
2. Use Run workflow or push an Android file change to main.
3. After success, download the booking-android-debug-apk artifact.
4. Extract and install app-debug.apk on POCO F4.

This produces a DEBUG/TEST build, not a signed production release.

## Approved behavior

- Manager generates a unique private booking invitation.
- Customer chooses Persian-calendar date and available time in a browser.
- Request remains pending; the manager explicitly approves or rejects it.
- Pending requests hold capacity temporarily (max 12h, ending 2h before appointment).
- Server serializes concurrent booking changes via MySQL InnoDB row locks.
- Customer checks result through a separate secret tracking link.
- SMS integration is deferred.

## Current scope

Implemented in code: login, share invitation, pending request review, approve/reject, simple manual booking, availability endpoint, tracking page and basic rate limiting.

Still to build: full Jalali native calendar editor, recurrence, client records, case notes, exact alarms, offline Room sync, advanced UX, signed production APK, and production security/hosting verification. **No backend has been deployed to hajarrajaee.ir.** Do not store real sensitive records until review and testing are completed.

See docs/DEPLOYMENT.md before any deployment.
