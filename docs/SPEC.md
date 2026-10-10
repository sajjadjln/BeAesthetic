# BeAesthetic: Product Spec (v0.1, draft for approval)

Status: **Draft.** Nothing in Phase 3 (design) starts until this is approved.
Items marked **[OPEN]** need your answer. Items marked **[DEFAULT]** are my choice, and you can override them.

---

## 1. Vision

> BeAesthetic is my personal Android coach for a 23-week transformation (17 Mehr 1405 to 29 Esfand 1405 / 9 Oct 2026 to 20 Mar 2027). Each day it tells me exactly what to do, from morning to night, and reminds me reliably, even on a Xiaomi and without Google services. Everything I do fills a GitHub-style dashboard grid: effort shows as green and bad days show as gaps, never red. A "minimum day" keeps me on the dashboard when life gets hard. Every Friday I close the week with a short review and plan the next one. Strength numbers prove progress in the first weeks; measurements and photos prove it over months. It works in Persian and English, with Jalali and Gregorian dates, and it backs itself up automatically so nothing is ever lost.

### Design principles (used to settle arguments)
1. **Today first.** The app's main job is to answer "what do I do now?" Everything else is secondary.
2. **One tap to log.** If logging something takes more than one tap, it needs a strong reason.
3. **Gaps, not failures.** Nothing is ever red, there are no streaks that reset, and there is no backlog of missed items. Coming back is always easy.
4. **Proof early.** Strength numbers and the dashboard show progress in weeks 1–4, before the mirror changes.
5. **Never lose data, never miss a reminder silently.** Reliability is a feature, not an afterthought.
6. **Small.** If a feature doesn't serve the 23-week goal, it isn't in the app.

## 2. The program timeline

| | Gregorian | Jalali | Weekday |
|---|---|---|---|
| Day 0: baseline and first Friday ritual | 9 Oct 2026 | 17 Mehr 1405 | Friday |
| Week 1 starts | 10 Oct 2026 | 18 Mehr 1405 | Saturday |
| Week 23 ends | 19 Mar 2027 | 28 Esfand 1405 | Friday |
| Finish line: final photos | 20 Mar 2027 | 29 Esfand 1405 (last day of the year) | Saturday |

- Weeks run **Saturday → Friday**. There are 23 full weeks (161 days), plus Day 0 and the finish line.
- **Check-ins** (photos and tape measurements) **once a month**, on a Friday morning: Day 0 (Mehr), then one per Jalali month (Aban, Azar, Dey, Bahman, Esfand), plus the finish line. That makes **7 photo sets**.
- The app must accept a **baseline entered after the fact** (weight, measurements, photos and strength tests dated Day 0), because the app won't exist on Day 0.

### Weekly rhythm

| Sat | Sun | Mon | Tue | Wed | Thu | Fri |
|---|---|---|---|---|---|---|
| Training | Rest | Training | Rest | Training | Rest | Hike + weekly ritual |

**[OPEN]** Your first message said "4-day/week home program (Sat/Mon/Wed/Fri)" and "hiking on Fridays". Is Friday's session the hike itself, or a home workout plus the hike? The table above assumes **the hike is Friday's session**.

## 3. Scope

### 3.1 MVP (what we build first, in slices)

| # | Feature | Notes |
|---|---|---|
| M1 | **Today screen** | The day split into blocks: Morning, Midday, Evening, Night. The block for the current time is expanded. Tap an item to complete it. |
| M2 | **Routines** | Daily items, items on specific weekdays, and **"every N days from the last time"** items. Each item can be marked non-negotiable and/or part of the minimum day. |
| M3 | **Minimum day** | A two-way switch at the top of Today: روز کامل (full day) / روز حداقلی (minimum day). Minimum shows only the floor items. Doing the floor guarantees the day at least the lightest green. |
| M4 | **Workout log** | Your program loaded from the plan you'll paste. Each session shows the last session's numbers, and each set is pre-filled with them. You log reps or seconds plus effort for each set. |
| M5 | **Strength tests** | Pull-ups, push-ups, plank, dead hang and one leg-health test. Every 2 weeks **[DEFAULT, confirm]**. |
| M6 | **Dashboard (داشبورد)** | A GitHub-style grid covering the whole program: green intensity = % of the day done, gray = nothing done, its own color for special days. Tap a square to see that day. |
| M7 | **Proof** | Weight (weekly), tape measurements and photos (every 4 weeks), strength numbers. Cards showing "since Day 0", plus simple line charts. |
| M8 | **Friday ritual** | Review (this week's row, wins, personal records) and then plan (special days, and a free-text snack note for each day that opens like a drop-down). About 10 minutes. |
| M9 | **Reminders** | One notification per block at fixed times, with separate Normal and Friday profiles. One follow-up only for non-negotiables. Fully local, no Google services. |
| M10 | **Reliability setup** | A setup screen for Xiaomi/MIUI/HyperOS and other aggressive phones (Autostart, battery settings), a test notification, and detection of reminders that fired late or not at all. |
| M11 | **Backup** | Automatic weekly zip (data and photos) into a folder you choose, synced to your PC with Syncthing. Plus manual export/import and a restore flow that has been tested. |
| M12 | **Bilingual** | Persian (default, right-to-left) and English. Jalali dates first with Gregorian underneath. Digits follow the language. |
| M14 | **Workout plan editor** | Settings → برنامه تمرین: pick a weekday, name the workout, add exercises (reps or seconds, sets, range), reorder by dragging. |
| M13 | **Onboarding** | Language → reminder times → reliability setup → backup folder → baseline entry (with past dates) → program import. |

### 3.2 Later (after the MVP is in daily use)

| Feature | Earliest | Why it waits |
|---|---|---|
| Goals: target values on metrics, with progress bars inside Proof | ~week 4 | Cheap, but you need a baseline and a few data points first |
| Before/after photo comparison slider | ~week 8 | Needs at least 2–3 photo sets to be worth anything |
| In-app camera with a "ghost" overlay of the previous photo (same pose and framing) | ~week 4, before the 2nd photo set | Makes photos comparable. In the MVP the photos are taken with the system camera |
| Weekly meal and snack planner with recipes | ~week 6 | A big feature. The MVP has a plain snack list |
| 5-week phases with new milestones | ~week 5 | Fights boredom in the middle of the program |
| Home-screen widget for Today | after MVP | A strong daily trigger, but it's polish |
| Sync to your own .NET backend | optional | Only if Syncthing backup isn't enough |

### 3.3 Deliberately out of scope
- Calorie and protein tracking. (Protein stays only as three checklist items, see §5.)
- A general calendar and a full task manager (projects, priorities). Simple one-off tasks are in scope (repeat = once).
- Accounts, cloud login, Firebase, Google Play Services, analytics and crash-reporting SDKs.
- Social features, sharing and leaderboards.
- Health Connect, Google Fit and wearables.
- An AI coach or generated workout plans.
- Play Store distribution. The app is installed as an APK.

## 4. Screens and navigation

The bottom navigation has 4 tabs (mirrored in right-to-left mode). Settings is opened from the top bar.

1. **Today** (امروز): the home screen.
   - Header: Jalali date with Gregorian underneath, the week number ("Week 3 of 23"), a settings gear, and the full/minimum day switch.
   - Blocks: Morning / Midday / Evening / Night, each with its items. Due "every N days" items show a small "due" label.
   - Workout item: opens the **Workout session** screen.
   - On Friday: a "Start weekly ritual" card.
   - On check-in Fridays: a "Check-in: weight, tape, photos" card.
   - After 2 or more days without opening the app: a "Welcome back" card. It never lists what you missed.
2. **Dashboard** (داشبورد): the full program grid, a weekly score for each row, a legend. Tap a day to see its items.
3. **Proof** (پیشرفت): "since Day 0" cards (weight Δ, waist Δ, best pull-ups, plank time…), the strength chart, weight and measurement charts, photo sets in a timeline.
4. **Week** (هفته): this week's plan (special days, the snack note for each day), plus the Friday ritual flow.
5. **Settings** (تنظیمات, gear on Today): reminder health, Tasks & routines, Reminders & alarms, Workout program, Backup, Language & calendar, Xiaomi setup.
   - **Tasks & routines**: every routine and one-off task, grouped by part of the day. Add/edit in a bottom sheet: name, part of day, repeat (every day / chosen weekdays / every N days / once), optional own reminder time, non-negotiable, in minimum day.
   - **Reminders & alarms**: block times for normal days and Friday, separate alarms for tasks with their own time, follow-up delay, Friday ritual time, a test button.

**Full-screen flows:**
- Workout session
- Strength test
- Check-in (weight → tape → 3 photos)
- Friday ritual (Review → Plan)
- Onboarding
- Reliability setup
- Backup and restore

### Key user flows

1. **Morning (target: under 30 seconds).** The 07:00 notification shows "Morning: 5 items". Tap it, Today opens on the Morning block, tick the items, done.
2. **Training session.** Today → Workout. For each exercise you see last session's numbers and today's target. Enter the reps or seconds for each set (pre-filled with the target, so usually one tap) and a quick effort rating. Finishing the session ticks the workout item.
3. **Night.** The 22:30 **[DEFAULT]** notification shows "Night: skincare, brush, prep tomorrow's snack: *oat balls*".
4. **Friday ritual.**
   - Review: this week's row, the weekly score, PRs and best routines (in a neutral tone).
   - Plan: mark special days in advance (for example travel) and write each day's snack note (any length, e.g. a full recipe).
   - The weekly backup runs when you finish.
5. **Check-in Friday.** In the morning: weigh-in, then tape (5 numbers), then 3 photos (front, side, back) with the same instructions every time.
6. **Bad day.** Open Today and switch to "روز حداقلی". Only the 3–4 floor items show. Do them and the square turns light green.
7. **Coming back after an absence.** The "Welcome back" card offers to "Start with a minimum day". Days you missed are just gray. There's no list of misses and no catch-up notifications.

## 5. Default routine set (editable in the app)

| Item | Block | Schedule | Non-negotiable | Minimum day |
|---|---|---|---|---|
| Morning skincare | Morning | Daily | ✔ | |
| Brush teeth (AM) | Morning | Daily | | ✔ |
| Creatine | Morning | Daily | | ✔ |
| Weigh-in | Morning | Fridays | | |
| Protein with breakfast **[OPEN: keep these 3?]** | Morning | Daily | | |
| Vitamin D (with a meal) | Midday | Daily **[confirm dose and frequency]** | | |
| Protein with lunch | Midday | Daily | | |
| Workout | Evening **[OPEN: what time do you train?]** | Sat/Mon/Wed | | |
| Hike | Morning | Fridays | | |
| Protein with dinner | Evening | Daily | | |
| Shower | Evening | Every 2 days from the last one | | |
| Shampoo | Evening | Every 3 days from the last one (≈ twice a week) | | |
| Night skincare | Night | Daily | ✔ | |
| Brush teeth (PM) | Night | Daily | ✔ | ✔ |
| Prepare tomorrow's snack (میان‌وعده) | Night | Daily (opens the day's snack note) | | |
| Nails | Evening | Every 14 days from the last time | | |
| Beard trim | Evening | Every N days **[OPEN: N?]** | | |
| Haircut | (any) | Every N days **[OPEN: N?]** | | |
| Minimum-day movement (e.g. 10 push-ups) | Evening | Only in minimum-day mode | | ✔ |

Non-negotiables are chosen per item in the app. The ✔ marks above are only my suggested starting set.

## 6. Rules

### 6.1 Which day an action counts for
- The day **rolls over at 04:00**, not at midnight **[DEFAULT]**. Brushing your teeth at 00:30 counts for the evening it belongs to.
- **Edit window:** you can tick or untick items for today and yesterday only. Older days are read-only, so the dashboard stays honest without becoming a source of guilt.

### 6.2 Day score and wall colors
- **Due items** for a day are:
  - every active routine scheduled for that weekday or day type, plus
  - "every N days" items that **first became due** that day.
  - Hidden minimum-day-only items don't count.
- `pct = done_due / total_due`.
- **Level** (wall intensity):
  - 0 (gray) = nothing done
  - 1 = anything done, **or** the minimum-day floor completed
  - 2 = 50% or more
  - 3 = 80% or more
  - 4 = 100%

  These thresholds are **[DEFAULT]** and will be tuned after 2 weeks of real use.
- **Special day** (sick, travel or off): the square gets its own color whatever was done, and the day is **excluded** from the weekly score. Limit: **2 per week** **[DEFAULT]**. It can be set in the moment or ahead of time in the Friday plan.
- **Weekly score**: the average `pct` over non-special days, shown as a percentage, plus "showed up X of Y days" (days at level 1 or higher).
- **No streaks.** The closest thing is "showed up X of the last 7 days", which is a rolling count and never resets to zero.

### 6.3 "Every N days from the last time" items
- `nextDue = lastDoneDate + N`. If an item has never been done, it is due from its start date.
- If you don't do an item on its due day, it stays visible in Today with an "overdue since …" label until you do it. It only counts in the score on **the first day it was due**, so lateness never piles up.
- Doing it on any day resets the clock from that day.

### 6.4 Workout progression **[OPEN: confirm option B from round 3]**
- Each exercise has a set count and a rep range (or a time range for holds).
- Each set is pre-filled with last session's numbers; no separate "target" is shown. When every set reaches the top of the range, the app suggests "+1 rep per set" or moving to the next (harder) version of the exercise. You confirm or ignore the suggestion; the app never changes your plan silently.
- Effort is logged per set on a 1–10 seek bar labelled «درجه سختی» (an RPE scale).
- A **personal record (PR)** is a new best on reps × sets for an exercise or a strength-test value. PRs show up in the Friday review and on Proof.

## 7. Reminders and notifications

### 7.1 Schedule

All times are **[DEFAULT]**, based on waking at 06:45. Bedtime is **[OPEN]**.

| Reminder | Normal days | Friday |
|---|---|---|
| Morning block | 07:00 | 07:00 (hike day, includes the weigh-in) |
| Midday block | 13:30 | 13:30 |
| Evening block | 19:00 | none (folded into the ritual) |
| Night block | 22:30 | 22:30 |
| Weekly ritual | n/a | 19:00 |
| Non-negotiable follow-up | +45 min after its block, **once** | same |

### 7.2 Behavior
- **One notification per block**, not one per item. It shows the pending count and the first few item names. Tapping it opens Today on that block.
- If a block has nothing pending, there's **no notification**.
- **Follow-ups:** only for items marked non-negotiable, only once, and only if the item is still not done. The follow-up has a "Done" action, so you can tick it from the notification without opening the app.
- **No catch-up.** A later block never repeats items from an earlier block. They stay visible in Today, but not in notifications.
- **No guilt messages.** Notifications never say "you missed…". Wording is neutral and forward-looking.
- **Absence:** after 3 days without opening the app, the block reminders reduce to Morning only, until you open the app again. That's less noise when you're away, and coming back is still easy.
- **Notification channels:** Blocks, Follow-ups, Ritual and Check-in, and System (backup and reliability warnings). Each one can be tuned in Android settings.

### 7.3 Reliability (Xiaomi first, any phone)
- Reminders use **exact alarms**. Because the app is installed as an APK, the Play Store rules that would block the most reliable alarm permission don't apply. The exact technique (`setAlarmClock` vs `setExactAndAllowWhileIdle`) is a Phase 4 decision.
- Alarms are **rescheduled** after:
  - a reboot
  - an app update
  - a time or time zone change
  - every app launch
- The **setup screen** opens the right system settings directly, with one-line instructions for each:
  - Notifications on
  - Autostart (Xiaomi)
  - Battery saver set to "No restrictions"
  - Lock the app in recents
  - Exact alarm permission

  It finishes with a test notification that fires 1 minute later.
- **Reminder health:** every alarm records when it was supposed to fire and when it actually fired. If a reminder fired more than 10 minutes late or not at all, Today shows a quiet banner with a "Fix reminders" link that opens the setup screen.

## 8. Backup and data safety

- **Storage:** everything is local. The database lives in the app's private storage, and photos are copied there (the app never keeps only a link to a gallery photo, so deleting it from the gallery can't lose it).
- **Automatic backup:** weekly, when the Friday ritual finishes, and after every check-in.
  - The backup is a zip containing a **JSON export of all data** (a readable, documented format that could later be imported by a .NET backend), a raw database copy, and all photos.
  - It is written to a folder you choose once, and that folder is synced to your PC by Syncthing.
  - The app keeps the last 8 backups in that folder.
- **Backup health:** Settings and Proof show "Last backup: 3 days ago". If the last backup is more than 10 days old, a gentle banner appears.
- **Restore:** import a zip, preview what it contains (dates, number of records, number of photos), then replace the current data. This is tested end to end before any photos are stored for real.
- **Manual export** at any time from Settings.

## 9. Data model (conceptual)

Dates are stored as Gregorian `LocalDate` (ISO). Jalali exists only in the presentation layer.

```
Routine           id, title, block, scheduleType(DAILY|WEEKDAYS|INTERVAL|ONCE|MIN_DAY_ONLY),
                  weekdays?, intervalDays?, onceDate?, ownReminderTime?, startDate,
                  isNonNegotiable, inMinimumDay, sortOrder, active
Completion        id, routineId, date, completedAt
DayMeta           date, specialType?(SICK|TRAVEL|OFF), minimumDayMode, note?
WeekPlan          weekStart(Sat), ritualCompletedAt?, reviewNote?
SnackNote         date, text                  -- free text, shown the night before as "prepare tomorrow's snack"

Exercise          id, nameFa, nameEn, kind(REPS|SECONDS), progressionNextId?
Workout           id, name, weekday           -- e.g. A=Sat, B=Mon, C=Wed
WorkoutExercise   workoutId, exerciseId, order, sets, rangeMin, rangeMax
Session           id, workoutId, date, startedAt, finishedAt?
SetLog            sessionId, exerciseId, setIndex, value(reps|seconds), effort(EASY|GOOD|HARD|MAX)

Metric            id, kind(BODY|TAPE|STRENGTH), nameFa, nameEn, unit(kg|cm|reps|s), higherIsBetter
MetricEntry       id, metricId, date, value, note?
PhotoSet          id, date, checkInIndex
Photo             photoSetId, pose(FRONT|SIDE|BACK), relativePath

ReminderSlot      profile(NORMAL|FRIDAY), block, time, enabled
ReminderLog       slotKey, scheduledAt, firedAt?
BackupRecord      at, fileName, sizeBytes, photoCount, ok
Settings          (key-value) language, digits, primaryCalendar, dayRolloverHour,
                  specialDayCapPerWeek, followUpMinutes, backupFolderUri, programStartDate
```

Derived values (day score, grid level, weekly score, next due date, PRs, "since Day 0" changes) are **computed, not stored**. They are pure functions, and they are where unit tests focus.

## 10. Localization
- Persian is the default and runs right-to-left; English is available. Every string comes from resource files and nothing is hard-coded.
- **Dates:** Jalali is the primary date with Gregorian underneath (for example "۱۸ مهر ۱۴۰۵ · 10 Oct 2026"). Week starts on Saturday.
- **Digits:** Persian digits in the Persian UI and Latin digits in the English UI. Number input accepts both.
- **Units:** kg and cm only.

## 11. Open items

1. **Bedtime** on normal days and Fridays. This sets the Night block time (default 22:30).
2. **Friday:** is the hike Friday's session, or is it a home workout plus the hike?
3. **Training time of day.** This decides which block the Workout item goes in.
4. **Workout progression:** option B (last session's numbers plus a suggested target)? You haven't answered this yet.
5. **Protein:** keep the three "protein with breakfast/lunch/dinner" ticks, or remove them?
6. **Strength tests every 2 weeks** (Saturday, before the session)? And which leg-health test?
7. **Beard and haircut intervals** (N days), and the vitamin D schedule.
8. **Your workout plan:** paste it when ready. It becomes the seed data.
9. **Your snack list:** 5–10 snacks you know how to make, to start with.
