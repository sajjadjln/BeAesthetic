# Lantern: Architecture

_Lantern v1, on the `lantern` branch (2026-10-28, D191). The v0.x architecture of «پیکر» is on `main`. This file keeps the parts that carry over unchanged (time rules, reminders, backup) and says what changed. The slice plan is `docs/LANTERN-ROADMAP.md`._

**What changed for Lantern, in short:**
- **App ID and package** `app.lantern` (debug `app.lantern.debug`), installed next to «پیکر». Version 0.9.0, 1.0.0 after the L9 checks.
- **Removed:** the workout, progress (metrics, check-ins, photos), letters, recaps, the week tab and ritual, the dashboard and the old character. Their code, tables and tests are gone.
- **Database:** a fresh schema: v1 with 8 tables, v2 adds `settings.character` and `settings.todayLayout`.
- **Theme:** one palette per character (D187), pixel shapes, the dialog box, character portraits. No accents, no light mode.
- **Language:** English and Persian (D174–D176), chosen in Settings, kept in plain preferences.
- **New UI:** the splash, onboarding, Today (two layouts), the task sheet, Settings and the Goals placeholder.

For product rules see `SPEC.md`; for screens see `DESIGN.md`; for decision history see `DECISIONS.md` (D30 onward).

---

## 1. Goals that drive every choice

1. **Reminders fire on a Xiaomi**, without Google services. This is the riskiest part, so it gets the most design.
2. **Data is never lost.** Schema changes never wipe data; backups are verified after they're written.
3. **The rules are testable on a plain JVM.** Scoring, due dates, reminder planning and Jalali dates are pure Kotlin with no Android in them.
4. **Small.** One developer, one user, one phone. No layer exists unless it pays for itself.

## 2. Stack

| Area | Choice | Why |
|---|---|---|
| Language | Kotlin 2.x, coroutines + Flow | Standard; the user knows it. |
| UI | Jetpack Compose, Material 3 as a base, our own theme and components | The design is custom (tiles, ring, grid). M3 gives us accessibility, sheets and switches for free. |
| Navigation | Navigation Compose with type-safe routes | Stable, small, enough for 4 tabs and a few full-screen flows. |
| Database | Room (SQLite) with KSP, exported schemas, auto-migrations | Reactive Flows, compile-checked queries, migration tests. |
| Settings | A single-row `settings` table **in Room**, not DataStore | One file holds everything, so backup and restore have one source. |
| Dates | `java.time` (native from API 26) + our own Jalali converter | No dependency. See §6. |
| Background | `AlarmManager` for reminders; WorkManager for the watchdog and backup jobs | Neither needs Google Play Services. WorkManager runs on JobScheduler. |
| DI | **Manual DI** (one `AppContainer`) | About 10 objects to wire. No annotation processing, no magic; receivers and workers just read `app.container`. Hilt also works without Google services, but costs build time for little gain here. |
| JSON | kotlinx.serialization | Backup format and nav routes. No reflection, R8-friendly. |
| Charts | Hand-drawn with Compose `Canvas` | We need one line chart, a sparkline and a grid. A chart library would be bigger than that code. |
| Locale | `AppLanguage`: the phone's language, or Persian or English from Settings | D174–D176. Persian: RTL, Persian digits, Jalali dates. English: LTR, Latin digits, Gregorian dates. Kept in plain preferences, because it's needed in `attachBaseContext`, before the database opens. |

**SDK levels:** `minSdk 26` (Android 8: native `java.time`, notification channels). `targetSdk` and `compileSdk` = 36, the newest platform on the laptop at slice 1. Exact versions are in `gradle/libs.versions.toml` (D89).

**Banned dependencies:** anything from `com.google.android.gms`, `com.google.firebase`, analytics or crash SDKs. A small Gradle check fails the build if one shows up in the dependency graph, even transitively.

## 3. Modules

Two Gradle modules:

```
:domain   Pure Kotlin (JVM) library. No Android imports.
          Models, rules, planners, Jalali, backup format.
          Fast unit tests that run anywhere, including the cloud container.
:app      Android application. Room, alarms, notifications, Compose UI,
          backup I/O, the AppContainer.
```

Why two and not one: the boundary is enforced by the compiler. Rules can't reach for `Context` by accident, and the tests that matter most run in seconds without the Android SDK.
Why not more: feature modules buy build speed and team boundaries. We have neither problem.

### Package layout

```
domain/src/main/kotlin/app/lantern/domain/
  time/        ProgramCalendar (04:00 rollover, part of day, week start), Jalali, JalaliMonth, Weekdays, ProgramDates (seed only)
  tasks/       Task model, due-list rules, interval due dates, task form rules
  score/       Day counts and levels (the day snapshot)
  reminders/   ReminderPlanner, ReminderHealth, block content
  backup/      Backup rules
  widget/      What the widget shows
  text/        PersianDigits (Latin in English)

app/src/main/kotlin/app/lantern/
  App.kt, AppContainer.kt, MainActivity.kt, AppLanguage.kt, ProgramClock.kt
  data/db/        Entities, DAOs, AppDatabase
  data/repo/      TaskRepository (the day snapshot), Seed
  reminders/      AlarmScheduler, receivers, notifications, Xiaomi setup
  backup/         Zip writer/reader, SAF folder, restore
  events/         Local usage log (D59)
  ui/theme/       Colors (a palette per character), Character (+ moods, layout), Pixel (shapes, sky, dialog box, portrait), Theme
  ui/splash/ ui/onboarding/ ui/today/ ui/tasks/ ui/settings/ ui/backup/ ui/goals/ ui/components/
  widget/         The Today widget (Glance)
```

Application ID: `app.lantern` (debug builds: `app.lantern.debug`). It can never change after the first install. «پیکر» keeps `app.beaesthetic`, so both can be installed (D191).

## 4. App architecture

One direction of data, MVVM style:

```
Room (Flow) ──► Repository ──► ViewModel ──► UiState (StateFlow) ──► Compose screen
                                   ▲                                       │
                                   └──────────── user events ◄────────────┘
                     domain functions (pure) are called inside the ViewModel
```

- **No use-case class per action.** A ViewModel calls repositories and pure domain functions directly. Use-case classes would be one-line wrappers.
- **One `UiState` data class per screen.** Screens are stateless composables that take the state and a few lambdas, so every screen has a Compose preview with fake data. A screen's only local state is UI state (a filter, the picked day, an open sheet). Each screen has a `…Route` that collects its ViewModel and a stateless `…Screen`.
- **One `ProgramClock`** wraps `java.time.Clock` and exposes "now", "program date" and a Flow that ticks at the next boundary (04:00 rollover and part-of-day changes). Tests and the debug menu replace it with a fake clock.
- **Debug menu** (debug builds only): set a fake "now", fire any reminder at once, run a backup, show the reminder log, show the usage log (D59), and switch the experiment toggles from `research/10-experiment-backlog.md` (D66). This is how reminder logic gets checked on the real phone without waiting for 22:30.

## 5. Time and day rules

All in `:domain/time`, all pure:

- `programDate(now) = (now − 4h).date`. The rollover hour comes from settings (default 4).
- `weekNumber(date)`: Week 1 starts Saturday 2026-10-10. Day 0 (2026-10-09) is week 0.
- **Current part of the day** comes from the reminder times of today's profile: Morning until the Midday time, Midday until Evening, Evening until Night, Night until the 04:00 rollover. On Friday (no Evening slot) Midday runs until Night.
- Weekdays are a 7-bit set with **Saturday = bit 0**, matching the UI order.
- `chapter(date)`: the Jalali month of the program date; Mehr and Aban are chapter 1, then Azar, Dey, Bahman, Esfand (`SPEC.md` §2, D51). Computed from the program dates in settings, never hard-coded, so a new program gets its own chapters.
- `landmark(date)`: chapter start, halfway (`halfwayDate` from settings, else the program's midpoint; the seed sets Yalda, 30 Azar = 21 Dec 2026) and finish (`programEndDate`).
- `isBeforeApp(date) = date < installDate` (D56).
- `countdown(date)`: `null` before the last chapter; otherwise days until `programEndDate + 1` and whether that day is 1 Farvardin (label «تا نوروز» vs «تا خط پایان») (D74).
- Time zone: the phone's zone. Iran dropped daylight saving time in 2022, but the code still handles zone and clock changes (see §7.4).

## 6. Jalali dates

**Choice: our own converter in `:domain`**, a Kotlin port of the `jalaali-js` algorithm (MIT, about 80 lines).

- Why not a library: the known Android libraries are small, old and Android-only; we need the conversion inside the pure JVM module. ICU's Persian calendar is not public API on Android.
- Why `jalaali-js`: it uses the official leap-year table (break years), so it matches the Iranian calendar, not just the 33-year approximation.
- **Tests:** the program's anchor dates (17 Mehr 1405 = 2026-10-09, 18 Mehr = 10-10, 28 Esfand = 2027-03-19, 29 Esfand 1405 = 2027-03-20, 1 Farvardin 1406 = 2027-03-21), Nowruz for 1400–1410, and a round trip over every day from 1395 to 1415.
- Formatting (month names, Persian digits) lives in `:app`, with month names in string resources.
- **Digits:** one small function maps 0–9 ↔ ۰–۹ for display; number input accepts both and normalizes. We don't rely on `NumberFormat`, which varies by Android version.

## 7. Reminders (the critical part)

### 7.1 Shape

```
ReminderPlanner (pure, :domain)
   input:  now, reminder slots, tasks, completions, settings, lastOpenedAt
   output: List<PlannedAlarm(key, at, kind)> for the next ~48 hours

AlarmScheduler.reconcile() (:app)
   1. ask the planner for the plan
   2. cancel PendingIntents whose key is no longer in the plan
   3. set the ones that are new or moved
   4. write one reminder_log row per planned alarm
```

- `reconcile()` is **idempotent**. It runs on: app start, every alarm fire, any task/reminder/settings edit, boot, app update, time or time-zone change, exact-alarm permission change, and a WorkManager watchdog every 6 hours.
- **Keys are stable**, e.g. `block:2026-10-10:MORNING`, `follow:2026-10-10:MORNING`, `task:42:2026-10-10`, `ritual:2026-10-16`. The key maps to the PendingIntent request code, so re-planning never duplicates alarms.
- **The notification content is built when the alarm fires, not when it's scheduled.** At fire time the receiver reads the database: pending items for that part of the day. Nothing pending → no notification (logged as "skipped, nothing pending").
- **Block content** lists each pending task with its anchor (D55) and leaves out tasks whose reminders are silenced (D57). A block with only silenced tasks pending shows nothing.
- **Follow-ups** are always planned at block + 45 min for blocks that contain important tasks. At fire time they show only important items that are still not done, with a "Done" action. Never more than one per block.
- **Task alarms** ring at the task's own time on its chosen weekdays, and are skipped at fire time if the task is already done.
- **Absence rule:** if `lastOpenedAt` is more than 3 days ago, the planner plans only Morning blocks. Opening the app re-plans everything.
- No catch-up: the planner never plans anything in the past.

### 7.2 Which AlarmManager call

| | `setAlarmClock` | `setExactAndAllowWhileIdle` |
|---|---|---|
| Fires in Doze | Yes, exactly | Yes, but at most about once per 9 minutes per app |
| Xiaomi battery saver | Treated like an alarm clock; the most robust option | Can be delayed or dropped unless the app is set to "No restrictions" |
| Visible side effect | An alarm icon in the status bar, and "next alarm" on the lock screen | None |

**Recommendation: `setAlarmClock` for every user-facing reminder** (blocks, follow-ups, task alarms, the ritual). Reliability is a stated product rule, and the icon is a small price. The user accepted the icon.
The scheduler has one switch (setting `alarmMode`), so we can move to `setExactAndAllowWhileIdle` later and let the reminder health log show whether it still holds on the phone.

### 7.3 Permissions

- `USE_EXACT_ALARM` (API 33+): granted at install and can't be revoked. Google Play limits it to alarm and calendar apps, but that rule doesn't apply to a sideloaded APK.
- `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"` for Android 12–12L.
- `POST_NOTIFICATIONS` (API 33+), asked during onboarding.
- `RECEIVE_BOOT_COMPLETED`.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: opens the system "don't optimize" prompt directly. Also Play-restricted, also fine for an APK.
- If exact alarms are ever unavailable, the scheduler falls back to inexact alarms and Today shows the "Fix reminders" banner.

### 7.4 Receivers

| Receiver | Listens to | Does |
|---|---|---|
| `ReminderReceiver` | our alarm PendingIntents | Logs the fire time, builds and posts (or skips) the notification, calls `reconcile()` |
| `NotificationActionReceiver` | "Done" on a follow-up | Writes the completion, updates the day snapshot, removes the notification |
| `SystemEventsReceiver` | `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` | Calls `reconcile()` |

Receivers use `goAsync()` and finish their database work within a few seconds.

**Locale in notifications:** receivers build notification text from a `Context` created with the `fa` locale, so notifications are Persian with Persian digits even when the phone is set to English.

### 7.5 Reminder health

- Each `reminder_log` row has `scheduledAt`, `firedAt` and an outcome: `SCHEDULED`, `SHOWN`, `SKIPPED_EMPTY`, `CANCELLED` (re-planned away) or `DEVICE_OFF`.
- **Health** (pure function): over the last 7 days, a row whose time is more than 10 minutes past with no fire is *missed*; a fire more than 10 minutes after its time is *late*.
- On boot, rows that fell inside the time the phone was off are marked `DEVICE_OFF` and don't count. The power-off window comes from the last log write before boot and the boot time.
- The watchdog worker also runs the health check. If something was missed, it posts **one** quiet System-channel notification per day ("Reminders may be blocked, tap to fix"). That way a broken setup is noticed even if the app isn't opened.
- Log rows older than 30 days are deleted.

### 7.6 Notification channels

Blocks (default importance, sound), Follow-ups (high), Ritual & check-in (default), System (low): backup and reliability warnings. Tapping a block notification opens Today on that part of the day.

### 7.7 Xiaomi setup screen

**Target phone: Poco X4 5G on HyperOS.** Poco uses the same Xiaomi security and battery apps, so HyperOS intents are tried first and tested on this phone.

Each row opens the right screen through an explicit intent, with fallbacks: MIUI and HyperOS component names differ between versions, so each row tries a list of known intents and ends with the app's system details page.

- Notifications on → app notification settings.
- Autostart → `com.miui.securitycenter` autostart manager (no public API can read this state, so the user confirms it with a tick).
- Battery "No restrictions" → `com.miui.powerkeeper` app config, then the standard battery-optimization prompt.
- Lock in recents → a picture of the gesture; it can't be opened by intent.
- Exact alarms → the system exact-alarm screen (API 31–32 only).
- Test → schedules a real alarm 1 minute ahead through the same path as every reminder, and checks it in the log.

On non-Xiaomi phones the Xiaomi-only rows are hidden.

## 8. Database (Room)

Lantern started a fresh schema (D191): `lantern.db`, version 1 in 0.1.0, version 2 in L1 (auto-migration). Dates are ISO `LocalDate` strings, times are minutes after midnight, instants are epoch milliseconds.

| Table | Columns | Notes |
|---|---|---|
| `task` | id, title, icon, part (MORNING·MIDDAY·EVENING·NIGHT), repeat (DAILY·WEEKDAYS·EVERY_N_DAYS·ONCE·MIN_DAY_ONLY·ANYTIME), weekdays (bits), everyNDays?, onceDate?, startDate, isImportant, inMinimumDay, action, alarmTime?, alarmWeekdays (bits), alarmEnabled, anchor?, remindersSilencedAt?, fadeOfferedAt?, sortOrder, createdAt, archivedAt? | Deleting a task archives it so history keeps its name. `anchor` = «بعد از…» (D55). ANYTIME = «هر وقت» (D127). `action` is always NONE in v1 (no workout). |
| `completion` | taskId, date, completedAt | Key (taskId, date). |
| `day` | date, special?, minimumMode, dueCount, doneCount, floorDone | The **day snapshot**, see below. |
| `reminder_slot` | profile (NORMAL·FRIDAY), part, time, enabled | |
| `reminder_log` | id, key, kind, scheduledAt, firedAt?, outcome | §7.5 |
| `event` | id, at, type, ref?, valueMinutes? | D59. The local usage log; never leaves the phone except in a backup. |
| `backup_record` | id, at, fileName, sizeBytes, photoCount, reason, ok, error? | Kept on the phone, never in a backup (D132). |
| `settings` | single row: the v0.x columns (program dates, accent and theme mode stay but are unused), plus v2: `character` (KNIGHT·VAMPIRE·WIZARD·GOTH, D186), `todayLayout` (LIST·PARTS, D188) | The language is not here (see §2). |

### The day snapshot (why some derived numbers are stored)

`SPEC.md` says derived values are computed, not stored. One exception is needed: if past days were scored from the *current* task list, then editing a task (say, moving shampoo to every 2 days) would recolor weeks of history.

So: whenever a completion, the minimum-day switch, a special day or the task list changes, the app recomputes **today's and yesterday's** `dueCount`, `doneCount` and `floorDone` with the pure scoring function and stores them in `day`. Once a day leaves the edit window (D17), its row is never rewritten. The level and colors are still computed from those counts at display time, so tuning the thresholds (D18) recolors history consistently.

A day the app never saw has no row, which is a gray gap, which is exactly right. The exception is a day before `installDate`: it renders as «قبل از اپ» (D56). The one-time baseline backfill writes `session`/`set_log` and `metric_entry` rows for those days, never `day` rows.

**Day detail** (tap a square) lists only what was done that day, read from `completion`. It never lists what wasn't done.

### Migrations

- Schemas are exported to `app/schemas/` and committed.
- Additive changes use Room auto-migrations; anything else gets a hand-written migration.
- Every hand-written migration needs a test that opens the old schema with real-looking data. Lantern has only the auto-migration 1 → 2 so far; the v0.x migration tests went with the old schema.
- **Never** `fallbackToDestructiveMigration`.

## 9. Backup and restore

### Format

`lantern-1405-07-24-193012.zip` (Jalali date and time to the second in the name, so it sorts naturally, never collides and reads easily; D133):

```
manifest.json     formatVersion, appVersion, dbSchemaVersion, createdAt, counts per table, photo list with sizes
data.json         every table as plain JSON, row by row (docs/BACKUP_FORMAT.md, D132)
photos/           0-front.jpg, 0-side.jpg … 6-back.jpg
db/lantern.db raw database copy, an emergency fallback only
```

`data.json` is the **source of truth for restore**. It has its own `formatVersion`, and the importer reads every older version. That's what lets an old backup restore into a newer app, and what a .NET backend could read later.

### Writing

1. Build the zip in the app's cache folder (database checkpointed first, so the copy is complete).
2. Re-open it and check the manifest counts against the database.
3. Copy it into the chosen folder as `….zip.partial`, then rename it. A half-written file never looks like a backup.
4. Delete the oldest backups beyond 8.
5. Write a `backup_record`.

**Folder:** chosen once with the system folder picker (Storage Access Framework); the app keeps a persistable permission. The user points it at a folder that **Syncthing-Fork** syncs to the PC. (The official Syncthing Android app was discontinued in late 2024; Syncthing-Fork continues it.)

**When:** at the end of the Friday ritual, after each check-in, from Settings by hand, and, as a safety net, on app open if the last backup is more than 7 days old. Since slice 8 the weekly one runs when the ritual finishes (D140). The automatic backup runs as a WorkManager job; a manual one runs in the app's scope. Either way, leaving the screen doesn't cut one off.

### Restore

1. Pick a zip with the system file picker.
2. Preview: date, app version, number of days logged, workouts, measurements, photos.
3. On confirm: **first make a backup of the current data**, then replace everything in one transaction, copy the photos, and re-plan reminders.
4. Anything that fails rolls back; the old data stays.

A test exports, wipes, restores and compares every table and photo (`BackupRoundTripTest`, Robolectric on the real schema), plus an older backup with missing tables and columns, a newer one refused, and the phone's own settings kept. A manual round trip on the phone (backup, restore from the file) confirms the folder side.

Android's own cloud backup (`allowBackup`) is **off**: it would send data to Google Drive, and it can't be trusted on phones without Google services anyway.

## 10. Photos

None in v1. The backup format still has a `photos/` folder, empty, so the format stays the same.

## 11. UI foundation

- **Theme (D187):** `lanternColors(character)` builds `BaColors` from the picked character's palette: the night sky, the glow (its color) and the dialog box's rims, with done green and due amber in every theme. `LanternTheme(character)` provides it, the type scale and `LocalCharacter` through `CompositionLocal`s, and maps M3's `colorScheme` from it. Changing the character (Settings, onboarding) recomposes the app at once.
- **Colors are tokens, never literals.** Done reads `accent`, the character's color reads `glow`, text on it `onGlow`.
- **Pixel pieces** (`ui/theme/Pixel.kt`): `PixelShape` (`Px.Card`, `Px.Small`, `Px.Dialog`), `Modifier.sky()`, `PixelDialog` (typing text), `CharacterPortrait` (round fade, glow, breathing).
- **Font:** Estedad static TTFs (400, 500, 600, 700, 800) in `res/font`, with the OFL license in the app's about screen. The type scale from `DESIGN.md` becomes `BaType`.
- **RTL:** `supportsRtl="true"`; layouts use start/end only. Charts and the dashboard grid read the layout direction, so time runs right → left in Persian.
- **Strings:** English in `values/`, Persian in `values-fa/`; every string lives in both (D174). Lint fails the build on a hard-coded string or a missing translation.
- **Locale:** `AppLanguage.wrap` sets the resources' locale and direction, and `PersianDigits.latin` for English. `Locale.setDefault` is left alone.
- **Motion (D76):** a `BaMotion` object in the theme holds the tokens from `DESIGN.md` → Motion (durations, the standard easing, and `spring()` specs for spring / settle / bouncy) and is provided through a `CompositionLocal`, like `BaColors`. Built only with Compose's animation APIs (`animate*AsState`, `Animatable`, `updateTransition`, `AnimatedContent`, `SharedTransitionLayout` for the workout tile → session), and `Canvas` for the ring, grid, charts and odometer. No Lottie. Compose's duration scale follows the system "Remove animations" setting, so motion becomes instant there; anything driven by hand reads the same scale. Vibration goes through one `Haptics` helper (light = `CONFIRM`, fallback `VIRTUAL_KEY`; medium = `EFFECT_HEAVY_CLICK`; record = a short waveform). No sound.
- **The characters (D184, D186):** portraits per mood are drawables cut from the sheets by `scripts/cut-character.swift` (480 px, the knight's embers recolored). `TodayPick` (unit-tested) decides the mood, the pool of lines and the «الان» task; the lines are string arrays per character and mood.
- **Accessibility (D68):** every icon-only tab and tile has a content description; layouts are checked at 130% font size; day detail states the level as a number, so the grid isn't color-only.
- **Crash log without an SDK:** an uncaught-exception handler writes the stack trace to a private file. Settings shows "last crash" with a share button. Nothing leaves the phone unless the user shares it.

## 12. Testing

| Level | What | Where it runs |
|---|---|---|
| Unit, `:domain` (most tests) | Jalali; program date and the 04:00 rollover; week numbers; chapters and landmarks; "before the app"; comebacks; routine strength and fade/restore rules; milestones; recap content; ladder rung suggestion and "rungs reached"; lifetime totals and round-number crossings; letter opening dates; guess outcome; countdown; BuddyPicker (pose per state, outfit per chapter, no line repeated within 7 days, no pose outside the moment's set); due lists for every repeat type; interval due dates and "counts only on the first due day"; day level thresholds; weekly score with special days and the cap; reminder planner (both profiles, follow-ups, task alarm weekdays, absence rule, nothing in the past); reminder health (late, missed, device off); set pre-fill; progression suggestion; PRs and the in-session record; "since Day 0" deltas; check-in schedule; backup JSON round trip and reading older format versions | Any JVM: Android Studio on the laptop, or a cloud session (`:domain` needs no Android SDK) |
| Unit, `:app` with Robolectric | DAOs on an in-memory database, the day snapshot writer, `AlarmScheduler.reconcile()` against a shadow AlarmManager, notification content | JVM |
| Unit, `:app` with Robolectric | Room migrations (`MigrationTestHelper`, schemas from the debug assets, D98) | JVM, every `./gradlew check` |
| Instrumented | Backup → wipe → restore round trip | An emulator or the phone, before each release |
| Manual, on the phone | A reminder checklist per release: fires at the right time with the screen off, after a reboot, after an update, with battery saver on | The user's Xiaomi |

No UI screenshot tests in the MVP. Compose previews with fake state cover visual review.

## 13. Build, release and install

- **Builds happen on the user's laptop** in Android Studio, which already has the Android SDK. The app is installed on the phone over USB (Run, or `adb install -r`, which keeps the data).
- Cloud sessions don't build the app: their container can't download the Android SDK. They can still edit code and run `:domain` tests, which need only a JVM.
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`). JDK 17 or newer.
- **No CI in the MVP.** A GitHub Actions workflow that runs the tests can be added later if it's wanted; it changes nothing in the code.
- **Signing:** Lantern uses the same release key as «پیکر» (a different app ID, so no clash). With the git-ignored `keystore.properties`, `./gradlew assembleRelease` signs on its own. The user created the release keystore once (`keytool`, at `~/peykar-release.jks`, alias `peykar`) and keeps it in a password manager and on a second disk. `scripts/release.sh` builds the signed APK: it asks for the password, passes it to Gradle as an environment variable for that one build (configuration cache off), and installs on a phone connected over USB. No password is ever stored in the repo or on disk. An update only installs over the app when it's signed with the same key, and that includes the later Bazaar release. Debug builds use the debug key and the `.debug` app ID, so they never clash with the real install.
- **Versions:** Lantern started at 0.1.0 (D191); 0.9.0 is the release candidate, 1.0.0 after the L9 checks. `versionCode` = major × 10000 + minor × 100 + patch.
- R8 minify and resource shrinking on release builds, with keep rules for kotlinx.serialization.
- A **banned-dependency check** runs as part of `./gradlew check` (no Google Play Services, Firebase or analytics).
- A **manifest check** fails the build if `android.permission.INTERNET` appears in the merged manifest (D60).

## 14. Build order

Lantern's slices L0–L9 and their status are in `docs/LANTERN-ROADMAP.md`. The v0.x slices are on `main`.

## 15. Later: a public release on Cafe Bazaar

**Not in the MVP.** Publishing on Cafe Bazaar is a **future goal, after the program** (D49). The research for it is in `research/07-marketing-strategy.md`. What we do now so that stays possible:

- **App ID `app.lantern` is final** (D191). Android package names allow only Latin letters, digits and `_`, so every app in Iranian stores has an English ID. Stores match searches against the listing (title, description), not the package name. The ID has no effect on search ranking (Bazaar's own app is `com.farsitel.bazaar`).
- **Nothing personal is hard-coded.** Program start and end dates live in `settings`; routines, the workout plan, metrics and reminder times are added by the user; a fresh install starts empty (D100).
- **The same release key** signs every build the user installs, so a store version can update the personal install later.
- **English is in the same app** (D174), chosen by the phone's language or in Settings.
- **The app name** is a string resource: «فانوس» / Lantern (D190). The icon is an adaptive vector pixel lantern with a monochrome layer.

Left for that release: onboarding that sets your own program dates and routine; the Bazaar listing (a Persian title with the words people search for, description, screenshots); a short privacy note (the app collects nothing and has no internet permission; the usage log never leaves the phone); starter routines for people without their own plan; and a check of Bazaar's current rules on target SDK and permissions such as `USE_EXACT_ALARM`.
