# VaghYar 1.5.2 beta18 — selected logo & design refresh

## Visual design rationale
- User-provided glossy hourglass with an orbiting cursor is the source of the new launcher and in-app brand icon; colors are harmonized to lilac, coral and mint.
- Reference flows: pastel Dribbble healthcare appointment concepts and the user's three-screen booking samples; simplify, do not clone copyrighted screens.
- The calendar remains the app home. The day/week/month switch is prominent and the day view has a compact Saturday-to-Friday selector with availability dots.
- The selection summary and new appointment CTA are visually separated from the schedule.
- The quick appointment sheet has a recognizable icon header and retains numeric hour/minute fields and existing storage/validation.

## Test and preservation
- New debug applicationId ends with `.beta18`, separate from beta17. Install alongside; old local records are NOT automatically visible.
- Export a password-protected `.vybk` backup from beta17; import it in beta18 and compare client/session/note counts.
- No backend or MySQL changes were made; online data remains server-side.
- Run on POCO F4: inspect icon sharpness, Persian Vazirmatn, calendar day selection, 5/6-week month transitions, booking creation and overlap rejection, and screenshot review before final.
- This is an experimental debug build; no release signing / automatic upgrades yet.

## User references
- Dribbble doctor-booking search
- Pastel booking screens shared in chat
- Hourglass brand artwork shared in chat and recolored for the launcher icon
