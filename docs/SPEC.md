# BeAesthetic: Product Spec (v0.3)

Status: **Accepted** (2026-10-09), updated with design-phase decisions and the research decisions (D48–D69, `research/`) and the gamification additions (D70–D75) on 2026-10-10. For screen layouts see `DESIGN.md`; for decision history see `DECISIONS.md`; for open questions see `TASKS.md`.
Items marked **[OPEN]** need the user's answer. Items marked **[DEFAULT]** are my choice and can be overridden.

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
- The app must accept a **baseline entered after the fact** (weight, measurements, photos and strength tests dated Day 0), because the app won't exist on Day 0. It also offers a one-time **backfill of workouts and strength tests** done since Day 0 (D56).

### Chapters and landmarks (D51, D52)

The program is split into five **chapters** that follow the Jalali months. A chapter start is a fresh start; it never resets any count. The monthly check-in falls inside each chapter as its proof point.

| Chapter | Jalali | Gregorian | Weeks |
|---|---|---|---|
| ① Mehr–Aban | 18 Mehr – 30 Aban | 10 Oct – 21 Nov 2026 | 1–7 |
| ② Azar | 1 – 30 Azar | 22 Nov – 21 Dec 2026 | 7–11 |
| ③ Dey | 1 – 30 Dey | 22 Dec 2026 – 20 Jan 2027 | 11–15 |
| ④ Bahman | 1 – 30 Bahman | 21 Jan – 19 Feb 2027 | 15–19 |
| ⑤ Esfand | 1 – 29 Esfand | 20 Feb – 20 Mar 2027 | 20–23 + finish |

- **Halfway: Yalda**, 30 Azar (21 Dec 2026), in week 11. The halfway recap opens the Dey chapter.
- **Finish: 29 Esfand**, the eve of Nowruz 1406. The finish recap, then the choice of what comes next (§6.5).

### Weekly rhythm

| Sat | Sun | Mon | Tue | Wed | Thu | Fri |
|---|---|---|---|---|---|---|
| Training | Rest | Training | Rest | Training | Rest | Hike + weekly ritual |

**[OPEN]** Your first message said "4-day/week home program (Sat/Mon/Wed/Fri)" and "hiking on Fridays". Is Friday's session the hike itself, or a home workout plus the hike? The table above assumes **the hike is Friday's session**.

## 3. Scope

### 3.1 MVP (what we build first, in slices)

| # | Feature | Notes |
|---|---|---|
| M1 | **Today screen** | A day ring, this week's dots, a part-of-day switcher (Morning, Midday, Evening, Night; the current part is selected) and icon tiles. Tap a tile to complete it. The header shows the week and chapter («هفته ۵ از ۲۳ · فصل آبان»). A tick gives a soft haptic and a short accent fill (D58). The first tick after a day at level 0 says «خوش برگشتی» once (D53). The character «پیکر» sits under the ring with one line that follows the day (D82). |
| M2 | **Tasks and routines** | Daily items, items on specific weekdays, **"every N days from the last time"** items, and one-off items ("once"). Each item can be marked important (UI: «مهم»; one follow-up reminder) and/or part of the minimum day, and can have its own alarm time on chosen weekdays. An optional anchor «بعد از…» says what the task follows (D55). |
| M3 | **Minimum day** | A leaf button on Today toggles «روز حداقلی». Minimum shows only the floor items. Doing the floor guarantees the day at least the lightest shade. |
| M4 | **Workout log** | Your program loaded from the plan you'll paste. Each session shows the last session's numbers, and each set is pre-filled with them. You log reps or seconds plus effort for each set. A set that beats the previous best shows «رکورد» at once (D54). Exercises sit on **ladders** of harder variants; reaching the top of the range offers the next rung (D70). The end of a session notes any round-number total crossed (D71). |
| M5 | **Strength tests** | Pull-ups, push-ups, plank, dead hang and one leg-health test. Every 2 weeks **[DEFAULT, confirm]**. Before each test, an optional guess; the result is shown next to it (D73). |
| M6 | **Dashboard (داشبورد)** | A GitHub-style grid covering the whole program: accent intensity (green by default) = % of the day done, gray = nothing done, its own color for special days. Days before the app was installed show as «قبل از اپ» (dotted), not gray (D56). Tap a square to see that day. |
| M7 | **Progress (پیشرفت)** | Weight (weekly), tape measurements and photos (**monthly**), strength numbers. Cards showing "since Day 0", plus simple line charts. A program line toward «نوروز ۱۴۰۶». 5–8 real-world milestones the user picks (D65). A ghost-overlay camera for the photos (D64). Ladders (D70) and lifetime totals (D71). After each check-in, a **reveal** of the new photos next to Day 0 with a compare slider (D75). |
| M8 | **Friday ritual** | Review (this week's row, wins, personal records) and then plan (special days, and a free-text snack note for each day that opens like a drop-down). About 10 minutes, or about a minute on the **short path**: review → start the week, with the plan steps optional (D62). Wins include comebacks and best routines (D53, D57). The identity sentence is shown (D65). |
| M9 | **Reminders** | One notification per block at fixed times, with separate Normal and Friday profiles. One follow-up only for non-negotiables. Fully local, no Google services. Notifications show each task's anchor. From week ~8 the app offers to silence reminders for routines that have stuck (D57). |
| M10 | **Reliability setup** | A setup screen for Xiaomi/MIUI/HyperOS and other aggressive phones (Autostart, battery settings), a test notification, and detection of reminders that fired late or not at all. |
| M11 | **Backup** | Automatic weekly zip (data and photos) into a folder you choose, synced to your PC with Syncthing. Plus manual export/import and a restore flow that has been tested. |
| M12 | **Bilingual** | Persian (default, right-to-left) and English. Jalali dates first with Gregorian underneath. Digits follow the language. |
| M14 | **Workout plan editor** | Settings → برنامه تمرین: pick a weekday, name the workout, add exercises (reps or seconds, sets, range), reorder by dragging. |
| M13 | **Onboarding** | Language → reminder times → reliability setup → backup folder → baseline entry (with past dates, plus the workout backfill) → program import → identity sentence and milestones. |
| M15 | **Chapters and recaps** | Five chapters by Jalali month; a halfway recap at Yalda (must ship by 19 Dec 2026) and a finish recap. In the app only (D51, D52, D63). Sealed **letters to your future self** at each chapter start (D72). A **countdown to Nowruz** on Today, in the last chapter only (D74). |
| M16 | **After the finish line** | The finish recap offers maintenance or a new program (§6.5). Built by ~week 18 (D61). |
| M17 | **Local usage log** | A private event log for judging experiments; never leaves the phone except inside the user's own backup (§8, D59). |
| M18 | **The character «پیکر»** | The logo's sticker guy as a gym buddy: small on Today, big at the moments, a new outfit each chapter. Rules in §6.8 (D82–D84). |

### 3.2 Later (after the MVP is in daily use)

| Feature | Earliest | Why it waits |
|---|---|---|
| Goals: target values on metrics, with progress bars inside Proof | ~week 4 | Cheap, but you need a baseline and a few data points first. The simple version, real-world milestones, is in the MVP (D65) |
| Weekly meal and snack planner with recipes | ~week 6 | A big feature. The MVP has a plain snack list |
| Home-screen widget for Today | after MVP; aimed at before week 8 if the pace allows (D67) | A strong daily trigger, but it's polish |
| Sync to your own .NET backend | optional | Only if Syncthing backup isn't enough |
| Public release on Cafe Bazaar | after the program (D49) | Needs onboarding with your own dates and starter routines, a listing and a privacy note (`ARCHITECTURE.md` §15, `research/07`) |

### 3.3 Deliberately out of scope
- Calorie and protein tracking. (Protein stays only as three checklist items, see §5.)
- A general calendar and a full task manager (projects, priorities). Simple one-off tasks are in scope (repeat = once).
- Accounts, cloud login, Firebase, Google Play Services, analytics and crash-reporting SDKs.
- Social features, sharing and leaderboards. Confirmed after the research: no share images either, recaps included (D63).
- Health Connect, Google Fit and wearables.
- An AI coach or generated workout plans.
- Play Store distribution. The app is installed as an APK.

## 4. Screens and navigation

The bottom navigation has 4 **icon-only** tabs (mirrored in right-to-left mode): Today · Dashboard · Progress · Week. Settings opens from the gear on Today. `DESIGN.md` has the current layout of every screen.

1. **Today** (امروز): the home screen.
   - Header: weekday, Jalali date, «هفته N از ۲۳ · فصل …», a minimum-day (leaf) button and a settings gear.
   - Ring of today's progress, 7 week dots, the «پیکر» row (the character and one line), part-of-day switcher, icon tiles. Due "every N days" items say «موعدش رسیده»; important items say «مهم».
   - Workout tile: opens the **Workout session** screen.
   - On Friday: a "Start weekly ritual" card.
   - On check-in Fridays: a "Check-in: weight, tape, photos" card.
   - After 2 or more days without opening the app: a "Welcome back" card. It never lists what you missed.
   - On the first day of a chapter: a quiet chapter card. At Yalda and on 29 Esfand: the recap card.
   - From week ~8, when a routine has stuck: a one-time offer to silence its reminders.
2. **Dashboard** (داشبورد): the full program grid, a weekly score for each row, a legend (including «قبل از اپ»). Tap a day to see its items.
3. **Proof** (پیشرفت): "since Day 0" cards (weight Δ, waist Δ, best pull-ups, plank time…), the program line toward Nowruz, milestones, the strength chart, weight and measurement charts, photo sets in a timeline.
4. **Week** (هفته): this week's plan (special days, the snack note for each day), plus the Friday ritual flow.
5. **Settings** (تنظیمات, gear on Today): reminder health, Tasks & routines, Reminders & alarms, Workout program, Backup, Appearance & language (accent color, dark/light, language), Xiaomi setup.
   - **Tasks & routines**: every routine and one-off task, grouped by part of the day. Add/edit in a bottom sheet: name, part of day, repeat (every day / chosen weekdays / every N days / once), optional own reminder time, optional anchor («بعد از…»), non-negotiable, in minimum day.
   - **Reminders & alarms**: block times for normal days and Friday, separate alarms for tasks with their own time, follow-up delay, Friday ritual time, a test button.

**Full-screen flows:**
- Workout session
- Strength test
- Check-in (weight → tape → 3 photos)
- Friday ritual (Review → Plan, or the short path)
- Recap (halfway and finish)
- Onboarding
- Reliability setup
- Backup and restore

### Key user flows

1. **Morning (target: under 30 seconds).** The 07:00 notification shows "Morning: 5 items". Tap it, Today opens on the Morning block, tick the items, done.
2. **Training session.** Today → Workout. For each exercise you see last session's numbers. Each set is pre-filled with last time's number, so logging is usually one tap; set the effort on the 1–10 «درجه سختی» bar. Finishing the session ticks the workout item.
3. **Night.** The 22:30 **[DEFAULT]** notification shows "Night: skincare, brush, prep tomorrow's snack: *oat balls*".
4. **Friday ritual.**
   - Review: this week's row, the weekly score, PRs and best routines (in a neutral tone).
   - Plan: mark special days in advance (for example travel) and write each day's snack note (any length, e.g. a full recipe).
   - **Short path:** review → «شروع هفته» in about a minute; the plan steps are optional.
   - The weekly backup runs when you finish.
5. **Check-in Friday.** In the morning: weigh-in, then tape (5 numbers), then 3 photos (front, side, back) with the ghost overlay of last month's photo and the same checklist every time: same spot and background, same light (window or lamp, no flash), phone at the same height and distance (timer or a fixed stand), same time of day (before breakfast), same clothes, relaxed pose. After the 3rd photo, the **reveal**: the new photos next to Day 0, with a slider (D75).
6. **Bad day.** Open Today and switch to "روز حداقلی". Only the 3–4 floor items show. Do them and the square turns the lightest shade.
7. **Coming back after an absence.** The "Welcome back" card offers to "Start with a minimum day". Days you missed are just gray. There's no list of misses and no catch-up notifications.
8. **Coming back after one missed day.** The first tick says «خوش برگشتی». That's all. On Friday the review lists it as a comeback.
9. **Finish line.** On 29 Esfand: final photos, the finish recap, then «ادامه» (maintenance) or «برنامه تازه» (new dates).

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
- **Edit window:** you can tick or untick items for today and yesterday only. Older days are read-only, so the dashboard stays honest without becoming a source of guilt. One exception: the one-time **backfill** at baseline entry, for workouts and strength tests only (D56).
- **Before the app:** days from Day 0 to the day before `installDate` have no due items and show as «قبل از اپ», never as gaps. They don't count in any score or in "showed up X of Y".
- **Frozen history [DEFAULT]:** a day's score is saved when it changes and frozen once the day leaves the edit window. Editing a task later never recolors past days (D37).

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

### 6.4 Workout progression
- Each exercise has a set count and a rep range (or a time range for holds).
- Each set is pre-filled with last session's numbers; no separate "target" is shown. When every set reaches the top of the range, the app suggests "+1 rep per set" or moving to the next (harder) version of the exercise. You confirm or ignore the suggestion; the app never changes your plan silently.
- Effort is logged per set on a 1–10 seek bar labelled «درجه سختی» (an RPE scale).
- A **personal record (PR)** is a new best on reps × sets for an exercise or a strength-test value. PRs show up in the Friday review and on Proof.
- **In the moment:** when a single logged set beats that exercise's best single set (or a strength test beats its best), the set circle shows «رکورد» right away (D54).

### 6.5 Chapters, recaps, comebacks and the finish (D51–D53, D61)
- **Chapter** = the Jalali month of the program date (§2); Mehr and Aban form chapter ①. Today's header line and the chapter card come from it.
- **Comeback** = a day at level ≥ 1 that follows a day at level 0 (special days and days «قبل از اپ» are not level 0). The «خوش برگشتی» line shows once, on that day's first tick.
- **Halfway recap** (Yalda) and **finish recap** (29 Esfand) show: days shown up so far, PRs, strength deltas since Day 0, milestones reached, the Day 0 photo next to the latest one, and the identity sentence. They lead with positives and never list misses.
- **After the finish:** «ادامه» keeps routines and reminders running with workouts optional and no chapters; «برنامه تازه» asks for new start and end dates and keeps tasks and the workout plan. History stays.

### 6.6 Routine strength and quieter reminders (D57)
- Each task has a **strength** in `:domain`: Loop Habit Tracker's formula, `multiplier = 0.5^(sqrt(frequency)/13)`, `strength = previous × multiplier + doneToday × (1 − multiplier)`, updated once per due day. For a daily task that's a ~13-day half-life.
- It picks the Friday review's "best routines". It is **never shown as a number** on Today.
- From week ~8: when a task's strength has stayed at or above **0.8** for 4 weeks, the app offers once to silence its reminders. A silenced task stays in Today and in every score but is left out of notifications. If it falls under 80% of due days over 7 days, the app offers to turn its reminders back on. Thresholds are **[DEFAULT]**.

### 6.7 Ladders, totals, letters, guesses and the countdown (D70–D74)
- **Ladder:** an ordered list of variants of one move (rung 1 = easiest). The workout uses the current rung. When every set of the current rung reaches the top of its range, the progression suggestion (§6.4) offers the next rung; accepting it swaps the exercise in the plan. Moving down is allowed. "Rungs reached" = the highest rung ever logged; it never decreases.
- **Totals:** sums over all logged sets since Day 0 (backfilled ones included): reps per ladder (every rung counts), seconds per timed move, finished workouts, days at level ≥ 1. Round numbers: reps 100 · 500 · 1,000 · 2,500 · 5,000 · 10,000; workouts 10 · 25 · 50 · 75; days 30 · 60 · 100 · 150. Each round number is noted once, when first crossed.
- **Letters:** a letter has a written date, an opening date and text. The first (offered once after install) opens on the finish day. A chapter letter opens at the next chapter start; the last chapter's opens on the finish day. A sealed letter can't be read early or edited; it can be deleted. On its day it appears as a card on Today.
- **Strength-test guess:** optional, pre-filled with the last result. If result > guess: «از حدست بهتر». Otherwise both numbers, neutral. No guess, no comparison.
- **Countdown:** from the first day of the last chapter, the header shows days until the day after `programEndDate` («N روز تا نوروز» when that day is 1 Farvardin, else «N روز تا خط پایان»). On the finish day: «امروز خط پایان», with «فردا نوروز» in front when it applies.
### 6.8 The character «پیکر» (D82–D84)
- **Pose and line** come from the state, in this order: minimum day or special day → tea; comeback (§6.5) → welcome, «خوش برگشتی»; day at 100% → flex; the selected part of the day done → cheer; no tick yet → stretch (morning) or the plain pose; after the night part → sleep; otherwise → thumbs up with the real count. Moments outside Today (record, chapter start, check-in, Friday wins, round numbers, recaps, finish, empty lists, onboarding) each have their own pose.
- **Lines:** several per moment in string resources; a line shown in the last 7 days is skipped while another is left. Numbers in a line are real data.
- **Outfit:** from the chapter of the program date (§2): ① none, ② sweatband, ③ beanie and scarf, ④ sunglasses, ⑤ sprout; Yalda and the finish day have their own.
- **Never:** a line or pose about a missed day, a sad or disappointed pose, a comment on the body or weight, a notification with the character.
- **Off switch** in Appearance; off hides the Today row and moments use their plain text.

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
- **One notification per block**, not one per item. It shows the pending count and the first few item names, each with its anchor when it has one (e.g. «اسکین‌کر · بعد از مسواک»). Tapping it opens Today on that block.
- Tasks whose reminders were silenced (§6.6) are left out of the count and the names.
- If a block has nothing pending, there's **no notification**.
- **Follow-ups:** only for items marked non-negotiable, only once, and only if the item is still not done. The follow-up has a "Done" action, so you can tick it from the notification without opening the app.
- **No catch-up.** A later block never repeats items from an earlier block. They stay visible in Today, but not in notifications.
- **No guilt messages.** Notifications never say "you missed…". Wording is neutral and forward-looking.
- **Absence:** after 3 days without opening the app, the block reminders reduce to Morning only, until you open the app again. That's less noise when you're away, and coming back is still easy.
- **Notification channels:** Blocks, Follow-ups, Ritual and Check-in, and System (backup and reliability warnings). Each one can be tuned in Android settings.

### 7.3 Reliability (Xiaomi first, any phone)
- Reminders use **exact alarms**. Because the app is installed as an APK, the Play Store rules that would block the most reliable alarm permission don't apply. The technique is in `ARCHITECTURE.md` §7 (proposed: `setAlarmClock`).
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
- **Usage log (D59):** the app records a few events locally (app opened, screen viewed, notification shown or tapped, minutes from a block notification to the first tick) so experiments can be judged. It never leaves the phone except inside the user's own backup, it's visible in the debug menu, and events older than 180 days are deleted. The app has **no internet permission** at all (D60).

## 9. Data model (conceptual)

Dates are stored as Gregorian `LocalDate` (ISO). Jalali exists only in the presentation layer.
This is the conceptual model from Phase 2. The physical Room schema, updated for D25–D28 and D37–D38, is in `ARCHITECTURE.md` §8 and wins where they differ.

```
Routine           id, title, block, scheduleType(DAILY|WEEKDAYS|INTERVAL|ONCE|MIN_DAY_ONLY),
                  weekdays?, intervalDays?, onceDate?, icon, startDate,
                  isImportant, inMinimumDay, sortOrder, active
TaskAlarm         routineId, time, weekdays(set), enabled
Completion        id, routineId, date, completedAt
DayMeta           date, specialType?(SICK|TRAVEL|OFF), minimumDayMode, note?
WeekPlan          weekStart(Sat), ritualCompletedAt?, reviewNote?
SnackNote         date, text                  -- free text, shown the night before as "prepare tomorrow's snack"

Exercise          id, nameFa, nameEn, kind(REPS|SECONDS), progressionNextId?
Workout           id, name, weekday           -- e.g. A=Sat, B=Mon, C=Wed
WorkoutExercise   workoutId, exerciseId, order, sets, rangeMin, rangeMax
Session           id, workoutId, date, startedAt, finishedAt?
SetLog            sessionId, exerciseId, setIndex, value(reps|seconds), effort(1..10)

Metric            id, kind(BODY|TAPE|STRENGTH), nameFa, nameEn, unit(kg|cm|reps|s), higherIsBetter
MetricEntry       id, metricId, date, value, note?
PhotoSet          id, date, checkInIndex      -- monthly: 0 = Day 0 ... 6 = finish line
Photo             photoSetId, pose(FRONT|SIDE|BACK), relativePath

ReminderSlot      profile(NORMAL|FRIDAY), block, time, enabled
ReminderLog       slotKey, scheduledAt, firedAt?
BackupRecord      at, fileName, sizeBytes, photoCount, ok
Settings          (key-value) language, digits, primaryCalendar, dayRolloverHour,
                  specialDayCapPerWeek, followUpMinutes, backupFolderUri, programStartDate
```

Derived values (day score, grid level, weekly score, next due date, PRs, "since Day 0" changes) are **computed, not stored**, except the frozen day snapshot (D37). They are pure functions, and they are where unit tests focus.

## 10. Localization
- Persian is the default and runs right-to-left; English is available. Every string comes from resource files and nothing is hard-coded.
- **Dates:** Jalali is the primary date with Gregorian underneath (for example "۱۸ مهر ۱۴۰۵ · 10 Oct 2026"). Week starts on Saturday.
- **Digits:** Persian digits in the Persian UI and Latin digits in the English UI. Number input accepts both.
- **Units:** kg and cm only.

## 11. Open items

Open questions are tracked in `docs/TASKS.md` (single source of truth).
