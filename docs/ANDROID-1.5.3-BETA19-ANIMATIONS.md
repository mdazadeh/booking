# VaghYar 1.5.3 beta19 — animation build / no hourglass

## Scope implemented
- Removed animated-hourglass concept and deleted its WebP image from the Android source. The existing pastel multicolor calendar-and-check vector (`ic_booking.xml`) is now used as the launcher and in-app logo.
- Animated slide/fade transitions when moving between Calendar, Clients, Requests, and More.
- Calendar day/section transitions using Crossfade and subtle expanding content.
- Material modal sheet transition for creating/editing appointments, plus fade/expand controls for date selector and recurrence settings.
- True asynchronous network progress indicator on the Requests and Server Sync panels (not a dummy timer).
- Booking recurrence: one appointment, weekly, every two weeks, Jalali monthly, or custom dates; all custom dates share the same clock time and duration, and are validated together for hours and conflicts.
- No database migration or live-host changes from this commit.

## Data and release safety
- This is a debug APK with distinct application id suffix `.beta19`; beta18 and its records remain untouched.
- Transfer client and appointment data only via the **encrypted `.vybk` backup/restore** within the More screen, and verify data before uninstalling any old beta.
- Backend and server DB unchanged. Running both app versions connected to the server simultaneously is not recommended until reconciliation.
- This release uses existing `ic_booking.xml` illustration. Earlier prerelease builds are archival and not modified.

## QA checklist for POCO F4
1. The launcher/login/header show no hourglass; calendar icon must be clear and colorful.
2. Tab switches animate without navigation glitches or losing data.
3. Date selection, opening/closing appointment sheet and recurrence expansion animate smoothly with no text overlap.
4. Requests and synchronization show a loader during actual network activity and hide it on success or failure.
5. Create a single appointment and weekly/monthly series; verify Persian dates and conflict errors.
6. Create several custom Jalali dates, all at one hour; ensure saving all-or-nothing and no duplicate/overlap.
7. Verify calendar day cards do not overlap at adjacent appointments.
8. Restore old encrypted backup and check all client, notes and appointment counts.
