# VaghYar Android 1.4 beta — migration and QA

## Scope
- Calendar-first screen, four-tab navigation and quick add/edit from calendar.
- New-client entry is embedded in the appointment sheet.
- Vazirmatn Regular / Medium / Bold bundled with the Android app (SIL OFL 1.1).
- Persian display digits with ASCII storage and API values.
- Paste-safe mobile input: Persian/Arabic/ASCII digits, LTR caret, +98 normalization.
- Local SQLite schema unchanged: `booking_local_1_2.db`, version 2.
- Server endpoints, SQL schema and local authentication not changed.

## Data safety / important APK signing issue
CI debug APKs are signed by ephemeral Android debug keystores, so direct updates from previous builds may be rejected by Android even when the applicationId is identical.
**This beta uses `.beta15` to install alongside `.beta14`**, without deleting old app data.
Old client records do **not** automatically appear in the new beta because it is a separate Android app identity.

Move local records safely:
1. Do **not** uninstall the old beta.
2. In old beta, go to More → encrypted backup, enter a **new backup passphrase of 12+ characters** and export to an accessible `.vybk` file.
3. Keep that backup file and passphrase safe; verify the file is saved successfully.
4. Install beta15 from GitHub Actions and sign in locally.
5. More → encrypted backup → enter the same backup passphrase → select the saved file → review **replace data** confirmation → restore.
6. Verify client count, sample appointments and notes, then test sync on one device at a time; avoid keeping two copies syncing simultaneously.
7. Do not remove the old APK until restored records and new backup have been checked.

Restoration **replaces** records in the receiving app (only), not data in the old beta.
The server account password and local app password are independent. Do not post either one publicly.

## Acceptance QA
1. Calendar is first screen; Tap any month day → quick sheet.
2. Tap empty hour in Day view → quick sheet with preselected time.
3. Select existing client and save appointment → calendar reflects it.
4. Create client inside quick sheet → client appears in Clients.
5. Try overlapping appointment → show conflict, do not replace existing appointment.
6. Tap appointment → edit time or cancel → calendar updates.
7. Paste phone `09123456789`, `۰۹۱۲۳۴۵۶۷۸۹`, `+989123456789`; verify stored canonical value and stable LTR display.
8. Confirm Persian dates and digits, including leap-year month navigation.
9. Export/restore `.vybk` to beta15; count and sample notes must match.
10. Test API sync on staging/test data and private-link approval. Avoid real sensitive records before security QA.

## Known work remaining
- Remote booking screen is still the earlier beta workflow; online UX redesign is not complete.
- Stable release signing and automatic in-place upgrades are not implemented.
- Offline-only behavior and integration must be validated on a real POCO F4.
- Multiple-session recurrence / moving whole series from the quick sheet is not yet exposed.
