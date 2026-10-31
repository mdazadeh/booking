# VaghYar 1.5.1 beta17 — UI and behavior fixes

## Completed
- Day calendar now uses measured, stacked time rows. Cards are no longer absolutely positioned, so time collisions cannot make cards overlap.
- Preexisting conflicting appointments are identified and separately visible with a warning. Local booking writes still use SQL transactions and interval conflict checking; server-side sync enforces its own slot lock.
- Separate numeric hours and minutes fields with a permanent colon in the middle. No need to type ':' using the phone keyboard.
- Larger Vazirmatn labels, colorful navigation, updated pink/lilac/mint/yellow calendar logo, pastel client list cards, rounded UI.
- Reminder lead time options scroll horizontally instead of clipping '120 minutes'.
- Removed break input and disabled legacy local break values. Local working hours are just start/end/day enabled.
- Delete client and associated local sessions and notes in a transaction; queue synchronized active sessions for cancellation on server, with persistent acknowledged tombstones to prevent server snapshot from recreating deleted records.
- Device-only reset has 2-step consent: checkbox asserting external encrypted backup and typing 'ریست'; clears clients, sessions, notes, pending deletions, work-hour local settings, and disconnects online session. It does NOT touch remote database. App login is retained.
- Local SQLite schema migration 3 -> 4 adds only `pending_remote_deletions`; existing records are retained. Portable encrypted backup includes deletion markers and supports old backup files.

## Critical data-safety reminders
This beta17 installs **alongside** beta16 because GitHub debug signing can change across builds; it does not automatically share old local records. In beta16: More -> Encrypted Backup -> export *.vybk, ensure file exists; in beta17 import using the same 12+ char passphrase. Verify client/session counts and notes before making changes. Do not uninstall beta16 until verification completes.

Deleting a client locally will queue remote cancellations. The source tracks completed cancellations to suppress reimport. If the server returns an authentication/version conflict or a cancellation failure, the remote appointment stays active until reconciled; check sync warnings. Do NOT delete a client while the only backup is missing or both app versions are syncing with the same server.

Device-only reset deliberately does not delete remote appointments or account; reconnecting later can import appointments again. For a full server purge, a separately approved server workflow and database backup is required.

## QA requirements for POCO F4
1. Create sessions 10:00–11:00 and 11:00–12:00; assert no conflict and no overlapping cards.
2. Import deliberate historical conflicts (in QA fixture) and confirm both cards visible plus a warning.
3. Reject 10:30–11:30 when 10:00–11:00 exists; unchanged existing booking.
4. Test hour and minute input with numeric-only keyboard, including 07:40.
5. Confirm 10/30/60/120 reminder chips all reachable.
6. Confirm settings show only day on/off, start/end; old break does not block new slots.
7. Delete client with multiple appointments, notes and a synchronized appointment. Verify local cascade, queue, successful remote cancel and no reappearance after snapshot. Verify network failure leaves queue pending.
8. Reset only after export, checkbox and typing 'ریست'; confirm remote records remain untouched and automatic sync is disconnected.
9. Verify logo, typographic scaling and navigation on POCO F4 at system font sizes 100% and 130%.
10. New release-signed APK and automatic in-place upgrades are not implemented; current release is debug beta.
