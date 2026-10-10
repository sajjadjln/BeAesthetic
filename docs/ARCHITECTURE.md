# BeAesthetic: Architecture (v0.2)

Status: **Accepted** (2026-10-10). The user approved every recommendation, except the install route: builds happen in Android Studio on the user's laptop (§13). Updated the same day for the research decisions D48–D69 (chapters, recaps, comebacks, anchors, routine strength, usage log, no `INTERNET`, new slice order) and the gamification additions D70–D75 (ladders, totals, letters, guesses, countdown, check-in reveal).
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
| Images | Coil 3 | Loads the progress photos from private storage. |
| Charts | Hand-drawn with Compose `Canvas` | We need one line chart, a sparkline and a grid. A chart library would be bigger than that code. |
| Language switch | AndroidX AppCompat per-app locales | Works back to API 26 and stores the choice itself. |

**SDK levels:** `minSdk 26` (Android 8: native `java.time`, notification channels). `targetSdk` and `compileSdk` = the latest stable SDK at slice 1.

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
domain/src/main/kotlin/app/beaesthetic/domain/
  time/        ProgramCalendar (04:00 rollover, week number, part of day, chapter, landmarks, countdown), Jalali, Weekday set
  tasks/       Task model, due-list rules, interval due dates, RoutineStrength (Loop score), fade rules
  score/       Day level, weekly score, special-day cap, comebacks, "before the app"
  reminders/   ReminderPlanner, ReminderHealth
  workout/     Set pre-fill, progression suggestion (incl. next ladder rung), personal records (incl. in-session record), ladders, lifetime totals + round numbers
  progress/    "Since Day 0" deltas, check-in schedule, milestones, recap content, letter opening dates, strength-test guess outcome
  buddy/       BuddyPicker: the character's pose, line id and outfit from the day state and moment (D82–D84)
  backup/      Export format (serializable models), format versions

app/src/main/kotlin/app/beaesthetic/
  App.kt, AppContainer.kt, MainActivity.kt
  data/db/        Entities, DAOs, migrations, AppDatabase
  data/repo/      Repositories: map entities ↔ domain models, write the day snapshot
  reminders/      AlarmScheduler, receivers, notifications, Xiaomi intents
  backup/         Zip writer/reader, SAF folder, restore
  photos/         Capture (incl. ghost-overlay camera), import, downscale
  events/         Local usage log writer (D59)
  ui/theme/       Tokens per accent × dark/light, Estedad typography, shapes
  ui/components/  Tile, Ring, WeekDots, GridSquare, Chip, SeekBar1to10, Buddy («پیکر»: pose vectors + bubble) …
  ui/today/ ui/dashboard/ ui/progress/ ui/week/ ui/workout/ ui/settings/ …
```

Application ID: `app.beaesthetic` (debug builds: `app.beaesthetic.debug`, so both can be installed side by side). It can never change after the first install, and it stays the same for a later Cafe Bazaar release (§15).

## 4. App architecture

One direction of data, MVVM style:

```
Room (Flow) ──► Repository ──► ViewModel ──► UiState (StateFlow) ──► Compose screen
                                   ▲                                       │
                                   └──────────── user events ◄────────────┘
                     domain functions (pure) are called inside the ViewModel
```

- **No use-case class per action.** A ViewModel calls repositories and pure domain functions directly. Use-case classes would be one-line wrappers.
- **One `UiState` data class per screen.** Screens are stateless composables that take the state and a few lambdas, so every screen has a Compose preview with fake data.
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

**Language in notifications:** before Android 13, per-app language only applies to activities. Receivers build notification text from a `Context` created with the saved language, so a Persian user never gets an English notification.

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

Physical schema for `SPEC.md` §9, updated for D25–D28. Dates are ISO `LocalDate` strings, times are minutes after midnight, instants are epoch milliseconds.

| Table | Columns | Notes |
|---|---|---|
| `task` | id, title, icon, part (MORNING·MIDDAY·EVENING·NIGHT), repeat (DAILY·WEEKDAYS·EVERY_N_DAYS·ONCE·MIN_DAY_ONLY), weekdays (bits), everyNDays?, onceDate?, startDate, isImportant, inMinimumDay, action (NONE·WORKOUT·HIKE·WEIGH_IN·SNACK_NOTE), alarmTime?, alarmWeekdays (bits), alarmEnabled, anchor?, remindersSilencedAt?, fadeOfferedAt?, sortOrder, createdAt, archivedAt? | "Routine" is renamed **task** to match the UI («کارها»). The task alarm (D25) is folded in: one alarm per task. Deleting a task archives it so history keeps its name. `anchor` = the «بعد از…» text (D55). `remindersSilencedAt` set = left out of notifications (D57). Strength is computed, not stored. |
| `completion` | taskId, date, completedAt | Key (taskId, date). |
| `day` | date, special (SICK·TRAVEL·OFF)?, minimumMode, dueCount, doneCount, floorDone | Replaces DayMeta. Holds the **day snapshot**, see below. A future row can hold only a planned special day. |
| `snack_note` | date, text, updatedAt | |
| `week` | weekStart (Saturday), ritualDoneAt?, reviewNote? | |
| `ladder` | id, name, sortOrder | D70. A move family, e.g. «شنا», «بارفیکس». Seeded: push-up and pull-up. |
| `exercise` | id, name, kind (REPS·SECONDS), ladderId?, rung?, archivedAt? | One `name`, typed in whichever language the user uses. The next harder variant = same ladder, `rung + 1` (replaces `nextExerciseId`, D70). |
| `workout` | id, weekday (unique), name | A = Sat, B = Mon, C = Wed. |
| `workout_exercise` | workoutId, position, exerciseId, sets, rangeMin, rangeMax | Key (workoutId, position). Drag-to-reorder rewrites positions. |
| `session` | id, workoutId, date, startedAt, finishedAt? | |
| `set_log` | sessionId, exerciseId, setIndex, value, effort (1–10) | D27: effort is per set, 1–10. |
| `metric` | id, kind (BODY·TAPE·STRENGTH), key, name, unit (kg·cm·reps·s), higherIsBetter, sortOrder | Seeded: weight, 5 tape sites (waist, chest, arm, thigh, shoulders; D85), the strength tests. |
| `metric_entry` | id, metricId, date, value, guess?, note? | Baseline entries simply carry the Day 0 date. `guess` = the strength-test guess (D73). |
| `check_in` | idx (0–6), date, doneAt? | D28: 7 monthly check-ins. |
| `photo` | checkInIdx, pose (FRONT·SIDE·BACK), fileName, takenAt | Key (checkInIdx, pose). |
| `reminder_slot` | profile (NORMAL·FRIDAY), part, time, enabled | |
| `reminder_log` | id, key, kind, scheduledAt, firedAt?, outcome | §7.5 |
| `backup_record` | id, at, fileName, sizeBytes, photoCount, ok, error? | |
| `milestone` | id, title, metricId?, target?, reachedAt?, sortOrder | D65. Linked to a metric when it has one (e.g. pull-ups ≥ 10); otherwise ticked by hand. |
| `event` | id, at, type (APP_OPEN·SCREEN·NOTIF_SHOWN·NOTIF_TAP·FIRST_TICK), ref?, valueMinutes? | D59. Local usage log. `ref` = screen name or reminder key. Rows older than 180 days are deleted. Included in backups. |
| `recap` | key (HALFWAY·FINISH), seenAt? | D52. The content is computed; only "seen" is stored. |
| `letter` | id, writtenAt, opensOn, text, openedAt? | D72. The UI refuses to show `text` before `opensOn`. Included in backups. |
| `round_number` | key (e.g. `reps:ladder:3:1000`, `workouts:50`, `days:100`), reachedOn, seenAt? | D71. Written when a total first crosses a round number, so it's noted once even if a log is later edited. Totals themselves are computed. |
| `settings` | single row: language, programStartDate, rolloverHour, specialCapPerWeek, followUpMinutes, alarmMode, accentColor (GREEN·BLUE·TURQUOISE·VIOLET·ORANGE), themeMode (SYSTEM·DARK·LIGHT), backupTreeUri, lastOpenedAt, lastBackupAt, onboardingDone, programEndDate, installDate, halfwayDate?, identitySentence?, afterFinishMode (NONE·MAINTENANCE·NEW_PROGRAM), experimentFlags (bits, debug) | `installDate` drives the «قبل از اپ» cells (D56). |

### The day snapshot (why some derived numbers are stored)

`SPEC.md` says derived values are computed, not stored. One exception is needed: if past days were scored from the *current* task list, then editing a task (say, moving shampoo to every 2 days) would recolor weeks of history.

So: whenever a completion, the minimum-day switch, a special day or the task list changes, the app recomputes **today's and yesterday's** `dueCount`, `doneCount` and `floorDone` with the pure scoring function and stores them in `day`. Once a day leaves the edit window (D17), its row is never rewritten. The level and colors are still computed from those counts at display time, so tuning the thresholds (D18) recolors history consistently.

A day the app never saw has no row, which is a gray gap, which is exactly right. The exception is a day before `installDate`: it renders as «قبل از اپ» (D56). The one-time baseline backfill writes `session`/`set_log` and `metric_entry` rows for those days, never `day` rows.

**Day detail** (tap a square) lists only what was done that day, read from `completion`. It never lists what wasn't done.

### Migrations

- Schemas are exported to `app/schemas/` and committed.
- Additive changes use Room auto-migrations; anything else gets a hand-written migration.
- Every migration has a test that opens the old schema with real-looking data and checks it after migrating.
- **Never** `fallbackToDestructiveMigration`.

## 9. Backup and restore

### Format

`beaesthetic-1405-07-24-1930.zip` (Jalali date in the name, so it sorts naturally and reads easily):

```
manifest.json     formatVersion, appVersion, dbSchemaVersion, createdAt, counts per table, photo list with sizes
data.json         every table as plain JSON (documented in docs/BACKUP_FORMAT.md, written in slice 6)
photos/           0-front.jpg, 0-side.jpg … 6-back.jpg
db/beaesthetic.db raw database copy, an emergency fallback only
```

`data.json` is the **source of truth for restore**. It has its own `formatVersion`, and the importer reads every older version. That's what lets an old backup restore into a newer app, and what a .NET backend could read later.

### Writing

1. Build the zip in the app's cache folder (database checkpointed first, so the copy is complete).
2. Re-open it and check the manifest counts against the database.
3. Copy it into the chosen folder as `….zip.partial`, then rename it. A half-written file never looks like a backup.
4. Delete the oldest backups beyond 8.
5. Write a `backup_record`.

**Folder:** chosen once with the system folder picker (Storage Access Framework); the app keeps a persistable permission. The user points it at a folder that **Syncthing-Fork** syncs to the PC. (The official Syncthing Android app was discontinued in late 2024; Syncthing-Fork continues it.)

**When:** at the end of the Friday ritual, after each check-in, from Settings by hand, and, as a safety net, on app open if the last backup is more than 7 days old. Backups run as a WorkManager job, so leaving the screen doesn't cut one off.

### Restore

1. Pick a zip with the system file picker.
2. Preview: date, app version, number of days logged, workouts, measurements, photos.
3. On confirm: **first make a backup of the current data**, then replace everything in one transaction, copy the photos, and re-plan reminders.
4. Anything that fails rolls back; the old data stays.

An instrumented test exports, wipes, restores and compares every table and photo. Slice 6 is not done until this test passes on the phone.

Android's own cloud backup (`allowBackup`) is **off**: it would send data to Google Drive, and it can't be trusted on phones without Google services anyway.

## 10. Photos

- Taken with the system camera (`TakePicture` into a file we own) or picked from the gallery. Either way, the app **copies** the photo into private storage: `files/photos/<checkIn>-<pose>.jpg`.
- Downscaled to 2560 px on the long edge, JPEG quality 90, location EXIF removed. 21 photos stay around 15–25 MB, so backups stay quick.
- Never written to the shared gallery. Photos leave the phone only inside backups.

## 11. UI foundation

- **Theme:** the tokens in `DESIGN.md` become a `BaColors` class, built from the neutral set for dark or light plus the chosen accent palette (5 accents × 2 modes, including the accent's overrides for special and amber). It's provided through a `CompositionLocal`, and M3's `colorScheme` is mapped from it so built-in components match. The accent and the mode (system/dark/light) are columns in `settings`; changing them recomposes the app at once. No dynamic (wallpaper) color.
- **Accent is a token, never a literal.** No code says "green"; every done/progress color reads `colors.accent*` or `colors.grid*`. A unit test checks contrast for every accent × mode.
- **Font:** Estedad static TTFs (400, 500, 600, 700, 800) in `res/font`, with the OFL license in the app's about screen. The type scale from `DESIGN.md` becomes `BaType`.
- **RTL:** `supportsRtl="true"`; layouts use start/end only. Charts and the dashboard grid read the layout direction, so time runs right → left in Persian.
- **Strings:** Persian in `values/` (the default), English in `values-en/`; the English draft is `docs/design/en.js` (D86). Lint fails the build on a missing translation or a hard-coded string.
- **First run** sets the app language to Persian, whatever the phone's language.
- **Motion (D76):** a `BaMotion` object in the theme holds the tokens from `DESIGN.md` → Motion (durations, the standard easing, and `spring()` specs for spring / settle / bouncy) and is provided through a `CompositionLocal`, like `BaColors`. Built only with Compose's animation APIs (`animate*AsState`, `Animatable`, `updateTransition`, `AnimatedContent`, `SharedTransitionLayout` for the workout tile → session), and `Canvas` for the ring, grid, charts and odometer. No Lottie. Compose's duration scale follows the system "Remove animations" setting, so motion becomes instant there; anything driven by hand reads the same scale. Vibration goes through one `Haptics` helper (light = `CONFIRM`, fallback `VIRTUAL_KEY`; medium = `EFFECT_HEAVY_CLICK`; record = a short waveform). No sound.
- **The character «پیکر» (D82):** one `ImageVector` per pose in `ui/components/buddy/`, built from the same parts as `docs/design/piker.js` (ink outline, white border, shadow, body). The body color is a parameter (the accent); outfits are a separate overlay vector. A pose change is `AnimatedContent` with the bouncy spring; the bubble fades. `:domain`'s `BuddyPicker` decides the pose, line and outfit; the composable only draws. Lines are string arrays; the last-shown date per line is kept in the usage log (D59).
- **Accessibility (D68):** every icon-only tab and tile has a content description; layouts are checked at 130% font size; day detail states the level as a number, so the grid isn't color-only.
- **Crash log without an SDK:** an uncaught-exception handler writes the stack trace to a private file. Settings shows "last crash" with a share button. Nothing leaves the phone unless the user shares it.

## 12. Testing

| Level | What | Where it runs |
|---|---|---|
| Unit, `:domain` (most tests) | Jalali; program date and the 04:00 rollover; week numbers; chapters and landmarks; "before the app"; comebacks; routine strength and fade/restore rules; milestones; recap content; ladder rung suggestion and "rungs reached"; lifetime totals and round-number crossings; letter opening dates; guess outcome; countdown; BuddyPicker (pose per state, outfit per chapter, no line repeated within 7 days, no pose outside the moment's set); due lists for every repeat type; interval due dates and "counts only on the first due day"; day level thresholds; weekly score with special days and the cap; reminder planner (both profiles, follow-ups, task alarm weekdays, absence rule, nothing in the past); reminder health (late, missed, device off); set pre-fill; progression suggestion; PRs and the in-session record; "since Day 0" deltas; check-in schedule; backup JSON round trip and reading older format versions | Any JVM: Android Studio on the laptop, or a cloud session (`:domain` needs no Android SDK) |
| Unit, `:app` with Robolectric | DAOs on an in-memory database, the day snapshot writer, `AlarmScheduler.reconcile()` against a shadow AlarmManager, notification content | JVM |
| Instrumented | Room migrations; backup → wipe → restore round trip | An emulator or the phone, before each release |
| Manual, on the phone | A reminder checklist per release: fires at the right time with the screen off, after a reboot, after an update, with battery saver on | The user's Xiaomi |

No UI screenshot tests in the MVP. Compose previews with fake state cover visual review.

## 13. Build, release and install

- **Builds happen on the user's laptop** in Android Studio, which already has the Android SDK. The app is installed on the phone over USB (Run, or `adb install -r`, which keeps the data).
- Cloud sessions don't build the app: their container can't download the Android SDK. They can still edit code and run `:domain` tests, which need only a JVM.
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`). JDK 17 or newer.
- **No CI in the MVP.** A GitHub Actions workflow that runs the tests can be added later if it's wanted; it changes nothing in the code.
- **Signing:** the user creates the release keystore once (`keytool`) and keeps it in a password manager and on a second disk. An update only installs over the app when it's signed with the same key, and that includes the later Bazaar release. Debug builds use the debug key and the `.debug` app ID, so they never clash with the real install.
- **Versions:** `versionName` = `0.<slice>.<fix>` until the MVP is complete, then `1.0.0`. `versionCode` = major × 10000 + minor × 100 + patch.
- R8 minify and resource shrinking on release builds, with keep rules for kotlinx.serialization.
- A **banned-dependency check** runs as part of `./gradlew check` (no Google Play Services, Firebase or analytics).
- A **manifest check** fails the build if `android.permission.INTERNET` appears in the merged manifest (D60). Coil is used without its network module.

## 14. Build order (Phase 5 slices)

Each slice ends with an installable APK, a short demo note, and a pause for feedback.

| # | Slice | Done when |
|---|---|---|
| 1 | Skeleton: both modules, theme (color and motion tokens, 5 accents, Estedad, RTL, both languages), haptics helper, Room with seeded tasks (`SPEC.md` §5, with default anchors), launcher and notification icons from `docs/design/logo/` (D81), **basic Today** (tiles, ticking, day snapshot), the **reminder core** (planner, `setAlarmClock` scheduler, receivers, channels, block notifications with anchors, reminder log), usage log, `installDate`, manifest check | Real block reminders arrive on the phone and ticks survive restarts (D50) |
| 2 | **Today complete** (ring, week dots, minimum day, week + chapter line and chapter-start card (with the letter offer; the first letter is offered once after install), the last-chapter countdown, tick feedback, the «پیکر» row with the day poses and the line picker (D82), comeback line) and **reminders complete** (follow-up "Done", task alarms, health + banner, absence rule, Xiaomi setup screen, Reminders screen) | A week of real reminders with a clean health log |
| 3 | Tasks list + task editor (repeat rules, task alarms, important, minimum day, anchor) | The user edits their own routine without a rebuild |
| 4 | Workout plan editor + workout session (with the in-session «رکورد», ladders and the next-rung offer, round numbers at the end of a session) | A real session is logged and the next one is pre-filled |
| 5 | Dashboard (grid with «قبل از اپ» cells, week rows, day detail) | The whole program is visible |
| 6 | Backup and restore | The round-trip test passes on the phone, and Syncthing picks up the zip |
| 7 | Progress (ladders, lifetime totals), monthly check-in with the **ghost-overlay camera** and the **reveal** with a compare slider (before ~11 Dec), strength tests with the guess, baseline entry with past dates + workout backfill, program line, milestones | Day 0 data is entered and the first deltas show |
| 8 | Friday review and plan (short path, comebacks, best routines, reserves wording, identity sentence, special days, snack notes, weekly backup) | The first real ritual is done in the app |
| 8b | **Halfway recap**. **Deadline: 19 Dec 2026** (numbers-only version if time is short) | The recap opens on Yalda |
| 9 | Onboarding | A fresh install walks through setup |
| 10 | Later, in this order: widget (aimed at before week 8, D67) · reminder fade offer (from week ~8, D57) · finish recap + after-the-finish mode (by ~week 18, D61) | |

The slices before 8b must leave room to hit 19 Dec. If they slip, build 8b before slice 8.

Onboarding is last: the user is the only user for now and is already set up by the seed data.

Every slice also runs the accessibility checklist (§11, D68).

## 15. Later: a public release on Cafe Bazaar

**Not in the MVP.** Publishing on Cafe Bazaar is a **future goal, after the program** (D49). The research for it is in `research/07-marketing-strategy.md`. What we do now so that stays possible:

- **App ID `app.beaesthetic` is final.** Android package names allow only Latin letters, digits and `_`, so every app in Iranian stores has an English ID. Stores match searches against the listing (title, description), not the package name. The ID has no effect on search ranking (Bazaar's own app is `com.farsitel.bazaar`).
- **Nothing personal is hard-coded.** Program start and end dates live in `settings`; routines, the workout plan and reminder times are seed data the user can edit.
- **The same release key** signs every build the user installs, so a store version can update the personal install later.
- **The app name** (launcher label) is a string resource: «پیکر» in Persian, "BeAesthetic" in English (D79). The icon is an adaptive vector icon with a monochrome layer and a white notification silhouette (D78).

Left for that release: onboarding that sets your own program dates and routine; the Bazaar listing (a Persian title with the words people search for, description, screenshots); a short privacy note (the app collects nothing and has no internet permission; the usage log never leaves the phone); starter routines for people without their own plan; and a check of Bazaar's current rules on target SDK and permissions such as `USE_EXACT_ALARM`.
