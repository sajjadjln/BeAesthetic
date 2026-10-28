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

## Decisions (confirmed 2026-10-28, D191)

| # | Question | Recommended | Why |
|---|---|---|---|
| Q1 | Application ID | ✅ **New: `app.lantern`**, installed next to «پیکر» | «پیکر» stays on the phone and keeps running the 23-week program until Lantern is ready. No risk to its data. |
| Q2 | Bringing tasks over | ✅ **No import.** Lantern starts empty | «پیکر» was never installed on anyone else's phone, so there is nothing to carry over. |
| Q3 | Splash length | ✅ **Full (~4 s) on the first launch; short (~1.2 s, same animation, faster) after** | A 4 s wait on every launch gets old fast. Tap skips it. |
| Q4 | Kotlin package | ✅ **Rename `app.beaesthetic` → `app.lantern` in L0** | One big rename now is cheaper than living with two names. |
| Q5 | Language | ✅ **Persian and English from v1** (D174), Persian first | New strings go into `values/` and `values-fa/` from the first slice, so nothing has to be translated later. |
| Q6 | Version | ✅ **Lantern starts at 0.1.0**, 1.0.0 when L9 is done | Its own line, apart from «پیکر» 0.10.x. |

## The slices

Each slice ends with a build on the Poco, a short "what works, what's next", and your feedback, as in Phase 5. `./gradlew check` stays green.

### L0. Clean the branch
- Rename: app name «فانوس» / Lantern, `applicationId` and package `app.lantern`, `rootProject.name`, launcher label.
- Remove the workout and v0.x-only features: `ui/workout`, `ui/progress`, `ui/letters`, `ui/recap`, `ui/week`, `ui/dashboard`, the «پیکر» rig (`ui/components/buddy`), `WorkoutRepository`, `ProgressRepository`, `PhotoStore`, the program seed and `ProgramClock` (Today uses the plain date), the related Room tables (a fresh database, version 1) and their `:domain` rules and tests.
- Keep: tasks, reminders, backup, phone setup, the Jalali calendar, the widget (restyled in L7).
- **Done when:** the app builds and runs with the old Today, tasks, reminders and backup only; the domain tests that are left pass.
- ✅ **Done 2026-10-28 (0.1.0).** `./gradlew check :app:assembleDebug` green, 114 tests. As built:
  - Package, `applicationId`, project and DB file renamed (`app.lantern`, `Lantern`, `lantern.db`); deep links `lantern://`; the app name in strings is «فانوس».
  - Removed: workout, progress/check-in/metrics, letters, recaps, week/ritual, dashboard, the «پیکر» rig, their repositories, `:domain` packages (`workout`, `progress`, `buddy`, `week`) and tests, the migration test and the old schemas. The database is a fresh version 1 with 8 tables.
  - Today keeps: the ring, the parts pager, minimum day, «هر وقت», the health and backup rows, «+». The week dots and the header's week line are gone.
  - Onboarding loses the program-dates step. Settings loses the workout row. Bottom bar: Today and Goals; Goals shows «در دست ساخت» (an early L8).
  - Kept on purpose for later slices: `ProgramClock` and `ProgramCalendar.programDate` (the 04:00 rollover), the program dates in `settings` (unused; cleaned up in L3), the Appearance screen (removed in L7), unused strings (cleaned in L9). The signing env names (`PEYKAR_*`) are unchanged so `scripts/release.sh` still works.

### L1. The theme and the characters
- A theme built from the four palettes (D187): night sky, glow, dialog box, ink; done green and due amber in every theme.
- Pixel shapes as Compose `Shape`s: stepped corners (6 dp and 3 dp), the pixel dialog box with its triple border.
- Character art as app resources: per character the 3 Today moods (calm, busy, happy) cut from the sheets, plus the portrait. The knight's embers recolored cyan **once, offline**, not at runtime.
- The picked character and the layout saved in settings (DataStore or the existing settings row).
- A debug screen to flip characters and see every component.
- **Done when:** switching the character in the debug screen re-themes it at once.
- ✅ **Done 2026-10-28 (0.1.0, not yet seen on the phone).** As built:
  - `ui/theme/Colors.kt`: one palette per character, mapped onto the existing tokens, so every screen follows it now. New tokens `glow`, `onGlow`, `sky2`, `rimLight`, `rimDeep`; `accent` is always the done green, `amber` the due amber. The five accents and light mode are gone, and so is the Appearance screen (D189, done early from L7).
  - `ui/theme/Character.kt`: `LanternCharacter`, `Mood` (calm, busy, happy) and `TodayLayout`.
  - `ui/theme/Pixel.kt`: `PixelShape` (`Px.Card` 6 dp, `Px.Small` 3 dp, `Px.Dialog` two 4 dp steps), `Modifier.sky()`, `PixelDialog` (rims, scanlines, typing, the nudging arrow), `CharacterPortrait` (round fade, glow, breathing).
  - Art: `res/drawable-nodpi/char_<character>_<mood>.jpg`, 480 px, cut by `scripts/cut-character.swift` (embers recolored there). A few faint dark-red specks are left on the knight's armor; the script's threshold can be tightened later.
  - Settings: `character` and `todayLayout` columns (DB v2, auto-migration from v1).
  - Debug menu → "Theme (Lantern L1)": four buttons save the character; the gallery shows the three moods, the dialog box and the color tokens.

### L2. Splash
- The system splash (Android 12+ API): the night color and a small lantern icon, held only until the first frame.
- Then the Compose "Gather" animation: stars, the four portraits drift in, the light at the center, «فانوس», "LANTERN", the line, the pixel bar. Full on first launch, short after (Q3).
- Respects "remove animations" in the phone's settings.
- **Done when:** cold start on the Poco looks like the mockup, with no white flash.
- ✅ **Done 2026-10-28 (not yet seen on the phone).** As built:
  - The window background is the night sky in every mode (the light `window_bg` and `values-night` are gone), so nothing flashes white.
  - Android 12+: `values-v31/themes.xml` sets the system splash to the night color and a pixel lantern (`drawable/ic_splash_lantern.xml`). No splash library, so no new dependency.
  - `ui/splash/Splash.kt`: the "Gather" timeline from `splash.html` (stars, the four portraits each in its own glow, the core of light, «فانوس», "LANTERN", «یه چراغ برای هر روز», the pixel bar). Full (4.4 s) until onboarding is done, then at 30% speed (~1.3 s). A tap skips it. Only on a cold start. "Remove animations" makes it instant (Compose's duration scale).
  - **Open:** the launcher icon is still the old «پیکر» sticker; it needs a lantern icon (small, could join L9). **Open:** Q5 (English from the first slice) is not started: `values/` still holds Persian only. Splitting it into `values/` (English) and `values-fa/` should come before L3, so the new screens' strings land in both.
- ✅ **Strings split 2026-10-28.** 414 unused strings (workout, letters, recaps, dashboard, the old character) removed; the 238 left are English in `values/` and Persian in `values-fa/`. The app still runs in Persian whatever the phone's language (`PersianLocale`) until the «زبان» switch in L7. Digits are still always Persian (`PersianDigits`); English digits come with that switch.

### L3. Onboarding
- ① Character pick (four pixel cards, intro lines, smile on second tap).
- ② Today layout (list or parts).
- ③ Phone setup: notifications, exact alarms, Xiaomi battery and autostart (the existing `PhoneSetup` steps, restyled).
- **Done when:** a fresh install goes splash → onboarding → Today with the picked character and layout.
- ✅ **Done 2026-10-28 (not yet seen on the phone).** As built: character → layout → reminders (with the notification permission) → phone setup → backup folder → done. The character step re-themes the app on each tap and the second tap on the picked card makes it smile; «بازگردانی از پشتیبان» sits under the main button. The layout step shows both layouts as pixel cards with a sketch, the character explaining the picked one. The night sky is behind every step, and the app's main and secondary buttons are now pixel buttons in the character's color. The old hello step is gone (the splash says hello). The old program-date columns stay in `settings`, unused and harmless; dropping them would need a manual migration for no gain.

### L4. Today, layout 1 (list)
- Header: the date in the theme color, Jalali first. No week row.
- The character with breathing and blinking, and the pixel dialog that types its line; tap for the next line.
- Moods from the day only: calm at the start, busy after the first tick, happy when all are done; at night the calm look. Never nervous, cross or asleep.
- The progress bar (one block per task), the «الان» card with one-tap tick, the parts of the day, «هر وقت».
- Bottom bar: Today, «+», Goals.
- **Done when:** a real day of tasks runs on it, and ticks and moods are right after the 04:00 rollover.
- ✅ **Built 2026-10-28 (not yet seen on the phone).** `ui/today/TodayList.kt`, shown when the layout is LIST and it isn't a minimum day (the minimum day keeps the old floor grid until L5). The character's lines are string arrays per character and mood in both languages (`lines_<character>_<calm|busy|happy|night>`), with the «الان» task filled in; a tap on the character or the box gives the next line. The rules (mood, which lines, which task is «الان») are in `TodayPick` with unit tests. «+» is a pixel button at the bottom end; moving it into the bottom bar's middle (as in the mockup) waits for the bar's restyle in L7. The «هر وقت» card, the health and backup rows still use their old look inside the new screen.

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
