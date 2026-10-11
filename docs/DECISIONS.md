# Decision log

Newest decisions override older ones. Each entry: what was decided, why, and its status.
Status: **Final** (agreed by the user) · **Default** (my choice, not yet confirmed; the user may override) · **Superseded** (replaced by a later entry).

## Phase 1: Discovery (2026-10-09)

| # | Decision | Why | Status |
|---|---|---|---|
| D1 | The app's job: a daily checklist first, plus weekly planning, a workout log, and visible proof of progress. | The user's failure modes are forgetting and quitting; a daily checklist attacks them every day. | Final |
| D2 | Today shows the whole day split into Morning / Midday / Evening / Night; the current part is in focus. | Routines naturally cluster at these times. | Final |
| D3 | Reminders are gentle by default, with **one** follow-up for tasks the user marks as important ("non-negotiable"). | If everything nags, the app gets muted; if nothing nags, brushing and skincare get forgotten. | Final |
| D4 | Day progress is shown as green intensity by % done. Missed = gray, **never red**. Special days (sick, travel, off) get their own color, max 2 per week. | The user's top risk is "a bad week makes me avoid looking". Red would cause exactly that. | Final (cap of 2 is Default) |
| D5 | A "minimum day" floor (e.g. brush, creatine, 10 push-ups) still counts as showing up. | Keeps the user in the game on bad days without a streak to lose. | Final |
| D6 | Top abandonment risks: "no visible change" and "a bad week makes me avoid looking". Fixes: strength numbers as early proof; gray gaps; weekly fresh start; welcome-back without listing misses. | Stated by the user. | Final |
| D7 | Weekly ritual on **Friday** (the user's weekend): review the week, then plan the next. Weeks run Saturday → Friday. | The user plans on their weekend; Friday evening closes the week. | Final |
| D8 | ~~Language: fully bilingual (Persian + English) with RTL. **Persian is the default.**~~ Jalali date first with Gregorian underneath. ~~Digits follow the language.~~ | User choice (I had recommended English-only first). | Bilingual part superseded by D87 |
| D9 | No dependency on Google Play Services. Local exact alarms. APK install. Xiaomi is the reliability target, any Android must work. | The user lives in Iran and uses a Xiaomi. | Final |
| D10 | Backup: automatic weekly zip into a folder synced to the PC with **Syncthing**; plus manual export/import. | Photos and history must never be lost; works without foreign servers. | Final |
| D11 | Cut: calorie/protein tracking, generic calendar, full task manager. | Don't serve the goal; friction. | Final (see D25 for one-off tasks) |
| D12 | Protein stays only as three checklist items (with breakfast / lunch / dinner). | Keeps the habit visible with zero logging. | Default, not confirmed |
| D13 | Repeating routines (nails, shower, shampoo, haircut) are counted **from the last time done**. A late item only counts against the day it was first due. | Matches real life; lateness never piles up. | Final |
| D14 | Reminder times are fixed clock times with separate **Normal** and **Friday** profiles. User wakes at 06:45. | Predictable and easy to trust. | Final |
| D15 | Vision paragraph agreed (see `SPEC.md` §1). | | Final |

## Phase 2: Spec (2026-10-09)

| # | Decision | Why | Status |
|---|---|---|---|
| D16 | Spec written in `docs/SPEC.md`. The user moved on to design ("create the design"), which was taken as approval; open items keep their defaults. | | Final |
| D17 | Day rolls over at 04:00; edit window is today + yesterday. | Late-night brushing counts for the right day; history stays honest. | Default |
| D18 | Day level thresholds: 0 nothing · 1 anything or floor done · 2 ≥ 50% · 3 ≥ 80% · 4 = 100%. Tune after 2 weeks. | | Default |
| D19 | After 3 days without opening the app, reminders reduce to Morning only. | Less noise when away; easy return. | Default |
| D20 | Strength tests every 2 weeks (Saturday, before the session). | Early, visible wins. | Default, not confirmed |

## Phase 3: Design (2026-10-09 → 2026-10-10)

| # | Decision | Why | Status |
|---|---|---|---|
| D21 | Visual direction "quiet strength": dark-first, green as the only accent, amber for due/important, indigo for special days, Vazirmatn as the only font. | Used at 06:45 and 22:30; green must always mean progress. | Final |
| D22 | Figma was used for the first design-system pass, then dropped: the Starter plan's MCP limit (20 calls/month) ran out. Design continues in **HTML mockups**. | Free, fast, viewable on the phone. | Final |
| D23 | The user rejected an external design tool (Stitch) result in favour of this design. | | Final |
| D24 | Round 2 changes: "Wall" (دیوار) renamed **Dashboard (داشبورد)**; weekly focus (تمرکز هفته) removed; snacks are called **میان‌وعده** and stored as a free-text note per day that opens like a drop-down; less helper text everywhere. | User feedback. | Final |
| D25 | **Where tasks/reminders are set:** a Settings screen (gear on Today) → Tasks, Reminders, Workout plan, Backup, Language, Phone setup. One-off tasks are back in scope as "repeat once". Tasks can have their own alarm time. | User asked where alarms and tasks are configured. | Final |
| D26 | Workout: no "today's target". Each set is pre-filled with last session's number; only "last time: 12, 12, 10" is shown. The log button just says «ثبت». | User feedback. | Final (supersedes the "target" idea in D-spec §6.4) |
| D27 | Round 3, the **combined design** (current): | User picked per screen. | Final (approved 2026-10-10) |
|  | · Today = minimal: day ring, 7 week dots, part-of-day icon switcher, big icon tiles. Due task → amber outline + «موعدش رسیده». Important task → gold outline + «مهم». Leaf button = minimum day. | | |
|  | · Bottom nav = **icons only**, 4 tabs: Today · Dashboard · Progress · Week. | | |
|  | · Dashboard = detailed grid + week rows. «میانگین هفته‌ها» removed. | | |
|  | · Progress = detailed (stat cards, chart, weight, photos). | | |
|  | · Workout = minimal big-number counter. Effort = a **1–10 seek bar** labelled «درجه سختی». | | |
|  | · Week (Friday) = detailed review + plan. | | |
|  | · Settings = minimal 6 tiles. Tasks = detailed list. Task editor = minimal + alarm (time + which weekdays it rings). Reminders = detailed. | | |
|  | · New screen: **Workout plan editor** (برنامه تمرین). | | |
| D28 | Photo and tape-measure check-ins are **monthly** (7 sets: Day 0, then one per Jalali month, plus the finish line), not every 4 weeks. | User request. | Final |
| D29 | The GitHub-style grid is no longer required on Today; it lives only on the Dashboard. | User: "you don't need to follow everything I said". | Final |
| D30 | **Phase 3 closed.** Design round 3 (`docs/design/screens.html`) is approved. Onboarding, check-in, strength test, backup/restore, day detail, notifications and the English layouts get designed during the build slice that needs them. | User: "Design round 3 is approved." | Final |

## Phase 4: Architecture (2026-10-10, `docs/ARCHITECTURE.md`)

| # | Decision | Why | Status |
|---|---|---|---|
| D31 | Native Kotlin + Jetpack Compose (M3 as a base, own theme), Room, coroutines/Flow, Navigation Compose, kotlinx.serialization, WorkManager, Coil. `minSdk 26`. No Google/Firebase/analytics dependency; a build check enforces it. | The user's expected stack; nothing in it needs Google Play Services. | Final |
| D32 | Two modules: `:domain` (pure Kotlin, all rules and planners) and `:app` (Android). | The compiler keeps Android out of the rules; the important tests run on any JVM, including the cloud container. | Final |
| D33 | Manual DI with one `AppContainer` instead of Hilt or Koin. | About 10 objects to wire; no annotation processing. | Final |
| D34 | Reminders: a pure planner produces the next ~48 h of alarms; an idempotent `reconcile()` applies them on every trigger (launch, fire, edit, boot, update, time change, 6-hourly watchdog). Content is built at fire time. | One code path, nothing duplicated, nothing stale. | Final |
| D35 | `setAlarmClock` for every user-facing reminder; `USE_EXACT_ALARM` (sideloaded APK). A setting can switch to `setExactAndAllowWhileIdle`. | Most robust on Xiaomi; the cost is a status-bar alarm icon. | Final (user accepted the icon) |
| D36 | Own Jalali converter (Kotlin port of `jalaali-js`) with tests. | Must live in the pure module; matches the official leap-year table. | Final |
| D37 | **Day snapshot:** today's and yesterday's due/done counts are stored in `day` and frozen once the day leaves the edit window. Editing a task never recolors history. Day detail lists only what was done. | Honest history without versioning tasks; fits "no list of misses". | Final |
| D38 | Settings live in a single-row Room table, not DataStore. "Routine" is renamed **task** in code. A task's alarm is stored on the task (one per task). | One file to back up; names match the UI. | Final |
| D39 | Backup restores from `data.json` (versioned format); the raw DB copy is only a fallback. Zips are verified, written as `.partial` then renamed; keep 8. Restore makes a backup of current data first. Android auto-backup is off. Syncthing-Fork syncs the folder. | Old backups must restore into newer app versions; a half-written file must never look valid. | Final |
| D40 | Photos are copied into private storage, downscaled to 2560 px, location EXIF removed. | Gallery deletions can't lose them; backups stay small. | Final |
| D41 | ~~CI publishes signed APKs to GitHub Releases.~~ Superseded by D43. | | Superseded |
| D42 | Build order: skeleton + Today → reminders → tasks editor → workout → dashboard → backup → progress/check-in → Friday ritual → onboarding (moved last). | Reminders are the most urgent value; the user is already set up by seed data. | Final (slices 1–2 re-split by D50) |
| D43 | **Builds happen in Android Studio on the user's laptop**, installed over USB. No CI in the MVP. Cloud sessions don't build the app. | User choice; the laptop already has the Android SDK. | Final |
| D44 | Target phone: **Poco X4 5G on HyperOS**. HyperOS intents for the setup screen are tried and tested first. | User's phone. | Final |
| D45 | A public release on **Cafe Bazaar** may come much later. App ID `app.beaesthetic` (English) stays; a Persian app name doesn't conflict with it, and the package name doesn't affect store search. Nothing personal is hard-coded (program dates in settings). | User plans it for later; keeps that door open at no cost now. | Final (D49: a future goal) |

## Phase 3 follow-up: type and color exploration (2026-10-10)

| # | Decision | Why | Status |
|---|---|---|---|
| D46 | Explore other fonts and a user-chosen accent color (`docs/design/type-and-color.html`). Fonts compared: Vazirmatn (current), Estedad, Sahel, IBM Plex Sans Arabic, Noto Kufi Arabic, Rubik; all OFL. Readex Pro and Tajawal were dropped: no Persian letters (پ چ ژ گ) or Persian digits. Accents: green, blue, turquoise, violet, picked in Settings → «ظاهر و زبان» (the «زبان» tile renamed). The accent replaces green as the color of done/progress; amber never changes; the special-day color moves away from the accent for blue (→ violet) and violet (→ cyan). **My recommendation:** Estedad as the only font; green as the default with all four accents selectable. | User request. | Superseded by D47 |
| D47 | **Estedad** is the only font. **Five accent colors**: green (default), blue, turquoise, violet, orange, picked in Settings → «ظاهر و زبان». The accent means done/progress. With orange, due/important turns **yellow** (amber is only 13° of hue from orange); with blue, special days turn violet; with violet, cyan. Palettes in `DESIGN.md`. `screens.html` now uses Estedad. | User choice ("estedad, all four colors, green default, also add orange"). Orange needed the yellow swap so «مهم» never looks like «done». | Final |

## Research follow-up (2026-10-10, `research/`)

Market, competitor and gamification research is in `research/` (start with `research/FINAL-REPORT.md`). The user's answer: "in future I will do it [Bazaar], local log ok, no sharing, accept all".

| # | Decision | Why | Status |
|---|---|---|---|
| D48 | Research accepted. Every recommendation in `research/FINAL-REPORT.md` is adopted, except the weekly share image (D63). | User: "accept all". | Final |
| D49 | A **Cafe Bazaar release is a future goal**, after the program. Still no release scope in the MVP; D45 keeps the door open. | User: "in future I will do it". | Final |
| D50 | **The first installable build includes reminders.** Slice 1 = skeleton + basic Today + the reminder core (planner, scheduler, receivers, block notifications, reminder log). Today's polish and the rest of reminders move to slice 2. | The program is already running; reminders are the value the user feels most (research F1). | Final (supersedes the slice 1–2 split in D42) |
| D51 | **Program chapters by Jalali month:** ① Mehr–Aban (10 Oct–21 Nov), ② Azar (22 Nov–21 Dec), ③ Dey (22 Dec–20 Jan), ④ Bahman (21 Jan–19 Feb), ⑤ Esfand (20 Feb–20 Mar). Today shows «هفته ۵ از ۲۳ · فصل آبان»; Progress shows a line to «نوروز ۱۴۰۶». A chapter start is a quiet fresh-start card. Chapters never reset any count. The monthly check-in is the chapter's proof point. | Motivation dips around halfway; fresh starts at landmarks; short goals feel closer (research R1). Replaces the backlog's "5-week phases". | Final |
| D52 | **Two recaps, in the app only:** halfway at **Yalda** (30 Azar = 21 Dec 2026, week 11; it opens the Dey chapter) and the finish on 29 Esfand. Days shown up, PRs, strength deltas, Day 0 vs latest photos, the identity sentence. No image export (D63). The halfway recap must ship **by 19 Dec**; a numbers-only version is the fallback. | Halfway is the motivational low point; recaps turn data into a story (research R8). | Final |
| D53 | **Comeback:** the first tick after a day at level 0 shows «خوش برگشتی» once. The miss is never named. Comebacks count as a win in the Friday review. | The best program in a 61k-person gym study rewarded returning after a miss (research R2). | Final |
| D54 | **In-session record:** when a logged set (or a strength test) beats the previous best, its circle shows «رکورد» right away. | Immediate proof; competence drives adherence (research R3). | Final |
| D55 | **Anchor cue:** a task has an optional «بعد از…» text (e.g. «بعد از صبحانه»). It shows in the block notification and the task's detail. Tiles stay 1–2 words. Seed tasks get default anchors. | When-then plans and event cues build automaticity; reminders alone don't (research R4). | Final |
| D56 | **Days before the app aren't gaps.** `installDate` is stored. Days from Day 0 to the day before install show in a dotted "before the app" style, not gray. Baseline entry offers a one-time backfill of **workouts and strength tests** since Day 0 (an exception to the edit window). | The first Dashboard view must not be weeks of gray (research R5). | Final |
| D57 | **Routine strength** (internal): Loop Habit Tracker's score (`0.5^(sqrt(freq)/13)` decay, a ~13-day half-life for daily tasks). It picks the Friday review's "best routines". From week ~8 the app **offers** to silence the reminders of a routine that has stayed strong for 4+ weeks. Silenced tasks stay in Today and in the score; if the task drops under 80% for 7 days, the app offers to turn its reminders back on. Never shown as a number on Today. | Forgiving consistency measure; less dependency on reminders (research R6). | Final |
| D58 | ~~**Completion feedback:** a soft haptic and a short accent fill on tick; a slightly bigger moment when a part of the day completes and when the day reaches 100%. Nothing more.~~ | The calm design risks feeling cold (research R7). | Superseded by D76 |
| D59 | **Local usage log:** an `event` table (app opened, screen viewed, notification shown/tapped, minutes from a block notification to the first tick). It never leaves the phone except inside the user's own backup, shows in the debug menu, and rows older than 180 days are deleted. This is not analytics in the product-rule sense: no SDK, no network. | Needed to judge the experiments in `research/10`. User: "local log ok". | Final |
| D60 | **No `INTERNET` permission.** The build fails if `android.permission.INTERNET` shows up in the merged manifest. | Makes "nothing leaves the phone" literal; a trust signal for a later release (research F12). | Final |
| D61 | **After the finish line:** the finish recap offers **maintenance** (routines continue, workouts optional, no chapters) or **a new program** (new dates, same tasks and plan). Built by ~week 18 (late February 2027). | Habits that depend on reminders stop when the reminders stop (research F3). | Final |
| D62 | **Friday ritual short path:** review → «شروع هفته ۵» in about a minute; the plan steps (special days, snack notes) stay available but optional. | Data-entry effort is the top reason people stop health apps (research F11). | Final |
| D63 | **No sharing**, confirmed. The weekly share image (research E10) is dropped, and recaps aren't exported as images. SPEC §3.3 is unchanged. | User: "no sharing". | Final |
| D64 | **Photo proof sooner:** the ghost-overlay camera moves from "later" to slice 7, targeted before the Azar check-in (~Fri 11 Dec). Until then a consistency checklist is used (same spot, light, distance, time, clothes), starting with the 2nd photo set (~Fri 6 Nov). | Comparable photos need the same framing (research F10). | Final |
| D65 | **Small gamification additions:** 5–8 **real-world milestones** the user picks (e.g. first 10 strict pull-ups, −3 cm waist), shown when reached and in the recaps; special days are shown as reserves («۲ روز ذخیره»); an **identity sentence** the user writes once, shown in the Friday ritual and the recaps. No points, levels, badges wall, currencies, avatars or random rewards. | Real progress over points (research R9, R10). | Final |
| D66 | **Experiments** E1–E9 in `research/10-experiment-backlog.md` run as single-person designs (on/off blocks, staggered starts, before/after landmarks), baselined on the user's first 2–3 weeks. Their toggles live in the debug menu. | Only way to tell if a change helps one user. | Final |
| D67 | **Home-screen widget** for Today: after the MVP, aimed at before week 8 if the build pace allows. | An always-visible cue (research F13; evidence is weak). | Final |
| D68 | **Accessibility checklist** in every slice: content descriptions on icon-only tabs and tiles, layouts that hold at 130% font size, grid levels also readable as numbers in day detail. | Cheap now; required for a release (research F14). | Final |
| D69 | ~~The **Persian app name** (open question 10) is picked with the future release in mind: it says what the app does and avoids the "looks/aesthetic" connotation.~~ | Research F15. | Superseded by D77 |

## Gamification additions (2026-10-10)

Proposed in the follow-up conversation; the user: "add all the top picks". All follow the rules: nothing goes down because of a miss, nothing leaves the phone, no points.

| # | Decision | Why | Status |
|---|---|---|---|
| D70 | **Exercise ladders.** Each exercise can sit on a ladder of harder variants (e.g. knee push-up → push-up → diamond → archer; dead hang → negative → band-assisted → strict pull-up). When every set reaches the top of its range, the existing suggestion (SPEC §6.4) offers the next rung; the user confirms. Progress shows each ladder: rungs reached in the accent, the current rung outlined, the next ones in text-3. Stepping back down is allowed and never hides a rung already reached. Seeded with a push-up and a pull-up ladder; editable in the workout plan editor. | Visible mastery that doesn't depend on the mirror and can't go backwards. | Final |
| D71 | **Lifetime totals.** Counters that only go up: total reps per ladder (all variants count), total hold time for timed moves, total workouts, total days shown up. Crossing a round number (reps 100 · 500 · 1,000 · 2,500 · 5,000 · 10,000; workouts 10 · 25 · 50 · 75; days shown up 30 · 60 · 100 · 150) is a quiet moment at the end of the session or on Today, a Friday win, and part of the recaps. | Safe "number goes up": a missed day never lowers it, unlike a streak. | Final |
| D72 | **Letters to your future self.** Optional, never nagged. The first letter is offered once after install and opens on the finish day (29 Esfand). At each chapter start the chapter card offers a new letter that opens at the next chapter start (the Esfand letter opens on the finish day). A letter is sealed: it can't be read before its day; it can be deleted. | Gives the program a story; makes chapter starts feel like moments. | Final |
| D73 | **Guess before a strength test.** Before each test an optional «حدس بزن» field, pre-filled with the last result. After logging, the result shows next to the guess («حدس: ۸ · واقعی: ۱۰»); beating it adds «از حدست بهتر». A lower result shows both numbers in neutral colors, nothing else. | Turns a chore into a small game; early on you'll usually beat yourself. | Final |
| D74 | **Countdown in the last chapter only.** From 1 Esfand, Today's header line becomes «۲۹ روز تا نوروز» (days to 1 Farvardin; on 29 Esfand: «فردا نوروز · امروز خط پایان»). For a program that doesn't end on 29 Esfand the label is «… تا خط پایان». Never shown before the last chapter. | The goal-gradient effect works near the end; an early countdown makes the end feel far. | Final |
| D75 | **Check-in reveal.** After the 3rd photo of a check-in, a reveal screen shows the new photos next to **Day 0** (last month via a toggle) with a compare slider. The ghost overlay still uses last month's photo, faint, only for framing. This pulls the backlog's before/after slider into slice 7. | A monthly moment of visible proof. | Final |

## Motion (2026-10-10)

| # | Decision | Why | Status |
|---|---|---|---|
| D76 | **Motion system: calm by default, generous at the moments that matter.** Three tiers: everyday taps under 250 ms; progress moments up to ~1 s; milestones 2–4 s, rare and skippable. Only the accent moves: gray days, misses and the amber «موعدش رسیده» never animate. Things that fill over time fill right → left. Every satisfying moment has a vibration (light / medium / a record pattern). **Compose animation APIs only** (no Lottie), motion tokens in the theme, **no sound**, and the phone's "Remove animations" setting makes everything instant. The full list is in `DESIGN.md` → Motion; the demo is `docs/design/motion.html` (https://claude.ai/artifact/SdjkRYt5z13an56aZ99huE). | User asked for Duolingo-like feel; picked every recommended option: calm + moments, Compose only, sound off, demo first. | Final (supersedes D58) |

## Name and logo (2026-10-10)

| # | Decision | Why | Status |
|---|---|---|---|
| D77 | ~~**Persian name: «آراسته شو»** (launcher label and in-app name). **English name stays "BeAesthetic"**: the two mean the same ("become well-groomed / aesthetic"). For a future store listing, the title carries the search words, e.g. «آراسته شو \| برنامه روزانه تناسب اندام، تمرین در خانه و روتین پوست»; people find new apps by what they do, not by name. Check Bazaar for clashes before publishing.~~ | User wanted a name tied to "aesthetic" that people can find later; picked «آراسته شو» over «تندیس», «آراسته» and «تن‌آرا». | Superseded by D79 (the store-title advice still applies) |
| D78 | **Logo process:** hand-built vector concepts, judged at real sizes and in context on a board (`docs/design/logo.html`, https://claude.ai/artifact/2o9iY6KqvcGFTbhYhFF6j9). Six concepts: «پ» monogram (recommended; it replaced the «آ» monogram when the name changed, D79), V-taper, bust on a plinth, rising steps, mirror, ring and figure. Icon: dark background (#121715) with the mark in the accent green; a one-color layer for themed icons; a white silhouette for the status bar. Two rounds: pick 1–2, then refine. | Vector survives 24 px and converts to Android vector drawables; context beats a white page. User picked every recommended option: six concepts, dark background. | Final (the concept itself is still open) |
| D80 | **Logo direction, round 2: a fun, cartoonish green sticker person** in a double-biceps pose (from the user's reference): chunky cartoon bodybuilder with a big head and grin, huge biceps, tiny waist, dark ink outline, white die-cut sticker border, slight tilt and two sparkles, on the dark icon background. Variants on the board: on a green tile, without a face, and a serious green badge. The round-1 concepts are dropped. | User sent a reference ("a sticker person that is green"), then asked for "more cartoonish and more fun". | Final (variant still to pick) |
| D81 | **Logo chosen: «کارتونی», the cartoon sticker** on the dark background, green by default. Exported to `docs/design/logo/`: `ic_launcher_background.svg`, `ic_launcher_foreground.svg`, `ic_launcher_monochrome.svg` (adaptive icon layers, 108 dp), `ic_notification.svg` (white status-bar silhouette), `icon-full.svg` and `icon-512.png` (store, later). Import them in Android Studio with Vector Asset in slice 1. | User: "go with the cartoon one". | Final |
| D79 | **Persian name: «پیکر»** ("physique"). English stays "BeAesthetic". Store title later: «پیکر \| برنامه روزانه تناسب اندام، تمرین در خانه و روتین پوست». The V-taper logo concept now matches the name directly. Known risk, accepted: in news language «پیکر» often means the body of someone who has died. | User: "change the name to پیکر". | Final (supersedes D77's Persian name) |

## The character (2026-10-10)

| # | Decision | Why | Status |
|---|---|---|---|
| D82 | **The logo's sticker guy becomes a character who lives in the app, named «پیکر» like the app** (as Duo is to Duolingo). A **hype gym buddy**: warm, a bit goofy, proud of you; short first-person Persian lines. **Presence:** small on Today, always, in a row under the ring and week dots (he says the old comeback line «خوش برگشتی» now); big at the moments: 100% day, record, comeback, chapter start, check-in and reveal, Friday wins, round numbers, recaps, finish; plus empty screens and onboarding. 17 poses from one rig (`docs/design/piker.js`), picked from the state of the day. His body takes the user's accent; the launcher icon stays green. Board: `docs/design/character.html` (https://claude.ai/artifact/Jf7hbWjUQTf39HRDcVkExZ). | The user wants the logo character to feel like a person in the app, like Duolingo's Duo. It answers research R7 (the calm design risks feeling cold) with warmth at the moments that matter. | Final |
| D83 | **A new outfit each chapter**, from the calendar only: ① Mehr–Aban as the logo, ② Azar a sweatband, Yalda a Hafez book, ③ Dey a beanie and scarf, ④ Bahman sunglasses, ⑤ Esfand a sprout on his head and sabzeh, finish day the Nowruz goldfish. Nothing is earned, bought or lost. | Novelty over the months (research: Fabulous journeys, Duolingo events) without an economy; it can never go backwards. | Final |
| D84 | **Guardrails for «پیکر»:** never sad, angry or disappointed at the user; never names, counts or hints at a missed day; praises actions (ticks, sets, records, showing up), **never the user's body, weight or looks**; not in notifications (no "come back" pushes); not a pet (no needs, health, levels, coins); no idle animation (pose changes only when something happens, a quick bouncy pop; "Remove animations" makes it instant); a tap gives a new line; lines don't repeat within 7 days; he can be turned off in «ظاهر و زبان» (on by default). | Keeps Duo's warmth and drops Duo's guilt (the non-negotiable no-shame rule) and Finch's chores (research `04`). He is not an avatar of the user, so D65's "no avatars" still holds. | Final |
| D85 | **The remaining screens are designed** in `docs/design/flows.html` (https://claude.ai/artifact/An8XmUuGo4cWrbHHhWMkom), list in `DESIGN.md` → Flows. Defaults chosen there: onboarding order language → dates → reminders (with bedtime) → phone setup → backup folder → Day 0 → workout plan → identity sentence and milestones; **5 tape sites: waist, chest, arm (right, flexed), thigh (right), shoulders** (confirmed by the user); strength-test order pull-ups → push-ups → plank → dead hang → leg test, 2 min rest; day detail lets today and yesterday be edited and a special day be set there; the check-in card uses the special-day indigo surface. In `character` poses the free arm now hangs flared out (the hand-on-hip read as a blob; drawn behind the body it looked detached). | User: "design the pages that are not designed" and "fix the hip arm pose". | Final (tape sites confirmed 2026-10-10) |
| D86 | ~~**English is a switch, not a second set of screens.** The mockups (`screens.html`, `flows.html`) have a «فارسی / English» switch that flips every phone LTR and translates it from one string table, `docs/design/en.js`, which becomes `values-en/strings.xml` in slice 1. English rules: Gregorian date first, Jalali month names transliterated, Latin digits, time left → right, the app is "BeAesthetic" and the character "Peykar". The separate E1–E2 phones are gone. Also fixed: week ranges start on Saturday (week 5 = 16–22 Aban = 7–13 Nov).~~ | User: "make the English versions of the remaining screens". One table can't drift from the Persian screens, and it's how the app itself will work (one layout, two string files). | Superseded by D87 (the Saturday week-range fix stays) |
| D87 | **The app is Persian only.** No in-app language choice, no English strings, no LTR layouts. The app always runs in the `fa` locale (RTL, Persian digits), whatever the phone's language. Strings stay in resources and nothing is hard-coded, so an **English version, if it ever comes, is a separate build** (a product flavor with its own strings, LTR, its own app ID and listing), not a switch. Removed: the «فارسی / English» switch and `en.js` from the mockups, the language choice in onboarding and in Settings (the screen is now «ظاهر», Appearance), the per-app-locale language switch in the stack. Jalali dates stay first with Gregorian second. | User: "I don't want it to be two languages; if I release an English version, I'll make a separate English build." | Final (supersedes D86 and the bilingual part of D8) |

## Build, slice 1 (2026-10-10)

| # | Decision | Why | Status |
|---|---|---|---|
| D88 | **Bedtime 23:30** on all days, so the Night block rings at **23:00** (half an hour before, flows.html O3), on normal days and Fridays. **Training is in the evening**: the workout tile sits in Evening and the Evening block stays at 19:00. | User's answers to open questions 1 and 3. | Final |
| D89 | **Slice 1 toolchain:** Gradle 8.14.3, AGP 8.13.2, Kotlin 2.3.20, KSP 2.3.6, Compose BOM 2026.01.01, Room 2.8.4, WorkManager 2.11.0, `compileSdk`/`targetSdk` 36 (the newest platform installed on the laptop). AGP 8.x rather than 9.x: it works with the installed Android Studio (2025.3) without the AGP 9 migration. | Stable versions that the laptop's Studio and SDK support today. Upgrading is a separate, small change. | Default |
| D90 | **Seed defaults for open questions:** beard every 7 days, haircut every 30 days (Q6); the Friday hike is the Friday session (Q2); protein stays as three ticks (Q4). Interval tasks start on the install day, so on day one they all show «موعدش رسیده» once; ticking one starts its clock. Editable in slice 3. | Keeps slice 1 moving; nothing here is hard to change. | Default |
| D91 | **Until slice 2, the gear on Today opens a debug menu (debug builds only)**: a real test alarm 1 minute ahead, "show the current block now", reconcile, and the reminder and usage logs. In release builds the gear is hidden until Settings exists. Block alarms that fire after a later part of the day has started are logged `SKIPPED_LATE` and not shown (no catch-up). | The user can check reminders on the phone without waiting for 23:00. | Default |

## Build, slice 2 (2026-10-11)

| # | Decision | Why | Status |
|---|---|---|---|
| D92 | **The chapter in Today's header is the Jalali month of the day** («فصل مهر», then «فصل آبان», «فصل آذر»…). Chapter ① still spans Mehr and Aban for chapter starts, outfits and letters; the first chapter-start card is 1 Azar. A short first month (under 15 program days) joins the next month's chapter. | Matches D51's example («هفته ۵ · فصل آبان») and flows L1/L3; the character board's «فصل مهر–آبان» was the odd one out. | Default |
| D93 | **«پیکر» is drawn by a Compose `Canvas` port of `piker.js`**, not one `ImageVector` per pose. Same shapes, same four passes (shadow, sticker border, ink, accent body), all 17 poses, props and the 5 chapter outfits. | One source of geometry, exactly like the mockups; 17 hand-made vectors would drift. The debug menu shows every pose for checking. | Default (supersedes the drawing line in ARCHITECTURE §11) |
| D94 | **A follow-up posts one notification per important task still pending in its block**, each with «انجام شد». Still only one follow-up alarm per block, at block + 45 min, and only if the task isn't done when it fires. | «انجام شد» has to tick one specific task; ticking several at once could mark something not done. | Default |
| D95 | **Cards on Today:** a letter on its day comes first; the chapter-start card can show the same day (a chapter letter always opens on the next chapter's first day). Cards take the «پیکر» row's place. The first-letter offer shows once after install; «بعداً» ends it for good. The chapter card's «بعداً» hides it for that day. | Found while testing: the letter and the chapter start collide every chapter. | Default |
| D96 | **Reminder health, quiet by design:** Today shows the banner when a reminder was late, missed or blocked *after* the user last opened «گوشی», or when notifications or exact alarms are off. Opening «گوشی» quiets it until something new happens. The watchdog posts its once-a-day System notification only for missed or blocked reminders, not late ones. Alarms that fell while the phone was off are marked `DEVICE_OFF` at boot. | A week-long banner over one late alarm would nag; a missed one is worth a notification, a late one isn't. | Default |
| D97 | **Settings:** the tiles for slices 3, 4 and 6 (کارها, برنامه تمرین, پشتیبان) show «به‌زودی». «ظاهر» applies a change at once, with no «ذخیره» button. In minimum-day mode, tiles with the same name get their part («مسواک صبح», «مسواک شب»). | Instant feedback beats a save step for color and mode; the floor shows all parts at once. | Default |
| D98 | **Testing additions:** Room migrations are tested on the JVM with Robolectric (`app/src/test`), reading the exported schemas from the debug variant's assets; unit tests run on the debug variant only. The debug menu can shift the clock to 1 Azar, 1 Esfand and 29 Esfand. | A migration test on every `./gradlew check`, without an emulator. | Default |
| D99 | **Phone setup fixes (0.2.1), after the first test on the Poco.** Reminder channels vibrate (new channel ids, since a channel's settings can't change once created), and the test notification goes through the Blocks channel, so it behaves exactly like a reminder. In «گوشی» the whole row opens the setting and the circle only shows the state; it can't be ticked by hand. States are read from the phone: notifications, exact alarms, battery (Android's own prompt first, then Xiaomi's «بدون محدودیت»), and autostart through Xiaomi's app-op where the phone allows it. Only what no API can read is asked: autostart when unreadable (on return from its screen) and the recents lock (a dialog that explains it). `SCHEDULE_EXACT_ALARM` is declared on every version, so the system «زنگ‌ها و یادآورها» switch exists if a phone refuses `USE_EXACT_ALARM`. | User: "no vibration", "you can just check the box without activating them". | Default |

## Superseded

- Weekly focus sentence (Phase 2) → removed in D24.
- "Today's target" in workouts → removed in D26.
- Effort as 4 words (Easy/Good/Hard/Max) → replaced by the 1–10 seek bar in D27.
- Photos every 4 weeks → monthly in D28.
- Minimum day as a bottom ghost button, then as a top switch → now a leaf icon button on Today (D27).
- Thursday planning (first draft) → Friday (D7).
- Vazirmatn as the only font (D21) → Estedad (D47).
- Green as the only accent (D21) → five user-chosen accents, green by default (D47).
- CI + GitHub Releases as the install route (D41) → Android Studio builds over USB (D43).
- Slice 1 = skeleton + Today, slice 2 = reminders (D42) → the first build already has the reminder core (D50).
- Backlog "5-week phases with new milestones" → chapters by Jalali month (D51) and real-world milestones (D65).
- Ghost-overlay camera "later, ~week 4" → slice 7, before the Azar check-in (D64).
- Backlog "before/after photo comparison slider (~week 8)" → part of the check-in reveal in slice 7 (D75).
- `exercise.nextExerciseId` (ARCHITECTURE §8) → ladders: `exercise.ladderId` + `rung` (D70).
- Completion feedback limited to the tick, a finished part of the day and a 100% day (D58) → the full motion system (D76).
- Persian name that avoids the "aesthetic" connotation (D69) → «آراسته شو», tied to "aesthetic" on purpose (D77).
- Persian name «آراسته شو» (D77) → «پیکر» (D79).
- Bilingual app with a language switch (D8, D86) → Persian only; English would be a separate build (D87).
- "No mascot" (DESIGN.md → Motion, D76) and "no pet; warm copy instead" (research `04` §6) → the character «پیکر» (D82–D84). Still no pet mechanics.
- One `ImageVector` per «پیکر» pose (ARCHITECTURE §11) → a Canvas port of the rig (D93).
- The comeback line «خوش برگشتی» under the ring (D53) → said by «پیکر» in his Today row (D82). The rule itself is unchanged.
