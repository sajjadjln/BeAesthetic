# Lantern v1: roadmap

_Written 2026-10-28 on the `lantern` branch._

## What Lantern v1 is

«فانوس» / Lantern (D190) is «پیکر» refactored into a to-do app with a companion. v1 has only:

1. **The splash** ("Gather", D190).
2. **Onboarding**: the character pick, then the Today layout (D186, D188).
3. **Today**: both layouts, list and parts slider (D186–D188), with the new-task sheet (D189).
4. **Settings** (D189), with the screens it opens: tasks, reminders, backup, phone setup.
5. **Goals**: a tab that shows «در دست ساخت» (in development). No goal flow yet.
6. **No workout.** No plan, session, check-in, metrics, progress, letters or recaps.

What stays from «پیکر» under the hood: tasks and their rules, reminders and the health check, backups, the Jalali calendar, the 04:00 day rollover. These work on the Poco today, so we keep the code and change the look.

Everything else stays on `main`, untouched.

The design reference is the mockups in `docs/design/`: `splash.html`, `character-pick.html`, `today-v5.html`, `settings-task.html`.

## Decisions to confirm first

My recommendations; the build follows them unless you change them.

| # | Question | Recommended | Why |
|---|---|---|---|
| Q1 | Application ID | **New: `app.lantern`**, installed next to «پیکر» | «پیکر» stays on the phone and keeps running the 23-week program until Lantern is ready. No risk to its data. |
| Q2 | Bringing tasks over | **Import from a «پیکر» backup zip** in onboarding (optional step) | Uses the backup format we already have. Workout and progress tables are skipped. |
| Q3 | Splash length | **Full (~4 s) on the first launch; short (~1.2 s, same animation, faster) after** | A 4 s wait on every launch gets old fast. Tap skips it. |
| Q4 | Kotlin package | **Rename `app.beaesthetic` → `app.lantern` in L0** | One big rename now is cheaper than living with two names. |
| Q5 | Language | **Persian and English from v1** (D174), Persian first | New strings go into `values/` and `values-fa/` from the first slice, so nothing has to be translated later. |
| Q6 | Version | **Lantern starts at 0.1.0**, 1.0.0 when L9 is done | Its own line, apart from «پیکر» 0.10.x. |

## The slices

Each slice ends with a build on the Poco, a short "what works, what's next", and your feedback, as in Phase 5. `./gradlew check` stays green.

### L0. Clean the branch
- Rename: app name «فانوس» / Lantern, `applicationId` and package `app.lantern`, `rootProject.name`, launcher label.
- Remove the workout and v0.x-only features: `ui/workout`, `ui/progress`, `ui/letters`, `ui/recap`, `ui/week`, `ui/dashboard`, the «پیکر» rig (`ui/components/buddy`), `WorkoutRepository`, `ProgressRepository`, `PhotoStore`, the program seed and `ProgramClock` (Today uses the plain date), the related Room tables (a fresh database, version 1) and their `:domain` rules and tests.
- Keep: tasks, reminders, backup, phone setup, the Jalali calendar, the widget (restyled in L7).
- **Done when:** the app builds and runs with the old Today, tasks, reminders and backup only; the domain tests that are left pass.

### L1. The theme and the characters
- A theme built from the four palettes (D187): night sky, glow, dialog box, ink; done green and due amber in every theme.
- Pixel shapes as Compose `Shape`s: stepped corners (6 dp and 3 dp), the pixel dialog box with its triple border.
- Character art as app resources: per character the 3 Today moods (calm, busy, happy) cut from the sheets, plus the portrait. The knight's embers recolored cyan **once, offline**, not at runtime.
- The picked character and the layout saved in settings (DataStore or the existing settings row).
- A debug screen to flip characters and see every component.
- **Done when:** switching the character in the debug screen re-themes it at once.

### L2. Splash
- The system splash (Android 12+ API): the night color and a small lantern icon, held only until the first frame.
- Then the Compose "Gather" animation: stars, the four portraits drift in, the light at the center, «فانوس», "LANTERN", the line, the pixel bar. Full on first launch, short after (Q3).
- Respects "remove animations" in the phone's settings.
- **Done when:** cold start on the Poco looks like the mockup, with no white flash.

### L3. Onboarding
- ① Character pick (four pixel cards, intro lines, smile on second tap).
- ② Today layout (list or parts).
- ③ Phone setup: notifications, exact alarms, Xiaomi battery and autostart (the existing `PhoneSetup` steps, restyled).
- ④ Optional: import tasks from a «پیکر» backup (Q2).
- **Done when:** a fresh install goes splash → onboarding → Today with the picked character and layout.

### L4. Today, layout 1 (list)
- Header: the date in the theme color, Jalali first. No week row.
- The character with breathing and blinking, and the pixel dialog that types its line; tap for the next line.
- Moods from the day only: calm at the start, busy after the first tick, happy when all are done; at night the calm look. Never nervous, cross or asleep.
- The progress bar (one block per task), the «الان» card with one-tap tick, the parts of the day, «هر وقت».
- Bottom bar: Today, «+», Goals.
- **Done when:** a real day of tasks runs on it, and ticks and moods are right after the 04:00 rollover.

### L5. Today, layout 2 (parts slider)
- The 4-part pixel bar, swipe pages with the indicator following the finger, opens on the clock's part, the done square, «هر وقت» under the pages.
- **Done when:** switching the layout in Settings swaps Today with no restart.

### L6. New task sheet
- The simple sheet (name, «کِی؟», the day/part line, «ذخیره») and «تنظیمات بیشتر» (part, «بعد از…», «زنگ» with day squares, «مهم»). No «هدف» row in v1.
- The character at the top, happy when saved.
- Same rules as today's editor (D128–D130); only the look changes.

### L7. Settings and its screens
- Settings: the health banner, «همراه», «صفحه‌ی امروز», «زبان», then the rows.
- Restyle the screens it opens: tasks list, reminders, backup, phone setup. Same behavior.
- The home-screen widget in the theme.

### L8. Goals placeholder
- The Goals tab shows the character with a line like «اینجا داره ساخته می‌شه» and a «در دست ساخت» tag. Nothing else.

### L9. Release 1.0.0
- Both languages checked on every screen, in both directions.
- A week of daily use on the Poco next to «پیکر»; reminder health check green the whole week.
- Backup and restore tested on a real backup.
- Signed APK, installed over USB.

## After v1 (not now)

Goals (the goal flow, already designed in `goal-step*.html`), the goals wall, the dashboard, plugins, the workout as a plugin, the store. They come back from `main` one by one.

## Order and size

L0 → L1 → L2 → L3 → L4 → L5 → L6 → L7 → L8 → L9. L1 is the base for everything after it. L4 is the biggest slice. L2 and L8 are small.
