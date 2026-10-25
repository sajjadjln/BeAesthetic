# BeAesthetic: Product Spec (v1.0, to-do + goals)

Status: **Draft** (2026-10-21). The app's identity changed from a 23-week workout program to a to-do + goals app (D148–D157). The previous spec (v0.3, the workout program) is in git history. For screen layouts see `DESIGN.md` (not yet updated for v1.0); for decision history see `DECISIONS.md`; for open questions see `TASKS.md`.
Items marked **[OPEN]** need the user's answer. Items marked **[DEFAULT]** are my choice and can be overridden.
«R§n» refers to the goal-setting research report (`~/Documents/aesthetic/research/research-goalsetting.pdf`, outside the repo).

---

## 1. Vision

> BeAesthetic is my personal Android app for two things: the small things I do every day, and the big things I want to reach. The to-do side tells me what to do now, from morning to night, and reminds me reliably, even on a Xiaomi and without Google services. The goals side helps me set a goal, break it into months, weeks and one small daily action, and see it move. Missed days are gray gaps, never red, and plans bend instead of breaking. Every Friday I close the week with a short review. It is in Persian, with Jalali and Gregorian dates, and it backs itself up automatically so nothing is ever lost.

### Design principles (used to settle arguments)
1. **Today first.** The home screen answers "what do I do now?" Goals reach Today as small actions, not as a second home screen.
2. **One tap to log.** A goal action ticks like any task. Amounts and notes are optional (long-press).
3. **Gaps, not failures.** Nothing is ever red, no streak resets, no list of misses. A miss is a planning problem to fix, never a verdict.
4. **Process before outcome.** The app shows what you did (sessions, pages, pitches), because results (kg, toman, scores) lag and are noisy.
5. **You own the plan.** The app asks questions, gives examples and checks quality. It never decides the goal or the breakdown.
6. **Short visits.** Success means less time in the app and more time doing the work. No engagement tricks.
7. **Never lose data, never miss a reminder silently.**

## 2. The two parts and how they meet

| | To-do | Goals |
|---|---|---|
| What | Routines and one-off tasks for the day, as built in slices 1–10 | Long and short goals, each with its own dates |
| Where | Today, Tasks (Settings), Dashboard | The «هدف‌ها» tab, the goal detail, the goal flow |
| Daily touch | Tiles on Today | The goal's daily actions, which **are tasks** linked to the goal |
| Weekly touch | The week's squares and special days | The weekly review on Friday |

A goal's daily action is a normal task with a `goalId`. It lives in a part of the day, can have an anchor («بعد از…») and an alarm, counts in the day ring, and ticks in one tap. Its ticks also feed the goal's weekly target. Deleting a goal asks whether to keep its actions as plain tasks.

## 3. Scope

### 3.1 Kept from the build (to-do side)
Today (ring, week dots, part-of-day pages, tiles, «مهم», «موعدش رسیده», «هر وقت», minimum day, «+ کار جدید»), the tasks list and editor, reminders and the reliability setup, the dashboard grid, special days, backup and restore, onboarding, the widget, the reminder fade offer, the usage log. Their rules are in §6–§8 and unchanged unless noted.

### 3.2 New (goals side)

| # | Feature | Notes |
|---|---|---|
| G1 | **Goal flow** | Set a goal in 8 short steps, under 6 minutes (§5.3). |
| G2 | **Breakdown** | Goal → monthly milestones → weekly target range → daily actions with cue and if-then backup (§5.4). Editable any time from the goal detail. |
| G3 | **Quality checks** | Local rules, no AI; one gentle gray card at a time, never blocking (§5.5). |
| G4 | **Goals tab** | One card per goal: icon, title, chapter path, this week's band. «بعداً» ideas in a folded list at the bottom. |
| G5 | **Goal detail** | Path, weekly bands, outcome line, milestones, 1–5 ratings, artifacts, letters, the plan (§5.6). The fitness template adds the workout tools (§5.11). |
| G6 | **Goal on Today** | A goal strip under the ring; goal actions as tiles with their cue (§5.6). |
| G7 | **Weekly review** | The Friday ritual, rebuilt around goals (§5.7). |
| G8 | **Monthly chapter review** | At each chapter end, about 10 minutes (§5.8). |
| G9 | **Misses and re-planning** | Flex days, gentle cards, re-plan offers (§5.9). |
| G10 | **Moments** | Week on plan, chapter done, halfway, finish with the letter (§5.10). |
| G11 | **Fitness template** | The existing workout features, attached to a goal (§5.11, D152). |
| G12 | **Migration** | The current program becomes the first goal (§10, D154). |

### 3.3 Removed
- The fixed app-wide program: Day 0, «هفته N از ۲۳», program chapters, the Nowruz countdown, the program line (they move to each goal).
- The character «پیکر» and its Today row (D151). The new characters are a cast of four: the armored knight (D182), the vampire, the elderly wizard and the goth woman (D184), as images and GIFs (D159), art in `characters/`. They are on no screen until the user says so (D183) **[OPEN: placement, and how the four are used]**.
- The Progress tab as its own tab (it becomes the goal detail).

### 3.3b Plugins (D163–D165)
- A **plugin** is a template for one goal, with its own screens on top of the app. The workout is the first one (§5.11 becomes the workout plugin).
- An **online store**, run by the user, lists reviewed plugins. It is the only online screen; the rest of the app works offline.
- Plugins can contain code written by other developers against published guidelines, standards and contracts. Each is reviewed before it's listed.
- Plugin code runs in Zipline inside an isolated process with no permissions (D166, D177), and the host draws all plugin UI with the app's components and tokens (D172, D178). Plugins are written in Kotlin (D179). Only the `:store` module uses the network (D167). The store screen is a WebView limited to the store domain (D171). The workout plugin is built in and off by default (D168); it's a native module and its data stays in its tables (D180). Removing a plugin keeps its data (D181). Free at first (D169).
- The full task, with the contract, runtime, store and backend: `docs/tasks/plugin-platform.md`. The research and the plan: `docs/research/plugin-platform-research.md`, `docs/tasks/plugin-platform-plan.md`.

### 3.4 Later
| Feature | Why it waits |
|---|---|
| The personal experiment plan (R§7: adherence, recovery latency, review completion, plan churn, automaticity) | Needs a few weeks of goal data; the usage log already records the events |
| More templates with tools (study, money) | Only if a real goal needs a tool the plain flow lacks |
| Sync to the user's .NET backend | Only if Syncthing backup isn't enough |
| Public release on Cafe Bazaar (D49) | After the app proves itself in daily use |

### 3.5 Deliberately out of scope
- Points, XP, coins, levels, loot, streak counters, punishing avatars (R§3.7).
- App-generated goals or plans, and an AI coach. The app checks; you decide.
- Social features, sharing, public commitments, leaderboards (D63).
- Projects, priorities, tags and subtasks for to-do items. A goal is the only hierarchy.
- Accounts, cloud, Firebase, Google Play Services, analytics and crash SDKs. The network is used by the plugin store only (D164).

## 4. Screens and navigation

**Updated by D160–D162 (this section still shows the D157 mapping below; the rows for Dashboard and Week are superseded):** the bottom bar has **two tabs, Today and Goals**. The Goals tab is a **wall** with goals pinned to it like notes (D161). The dashboard is a screen reached from the wall, shown once a goal exists. There is no Week tab and no month screen; where the weekly and monthly reviews open from is open (`TASKS.md`). Mockups: `docs/design/goals-v1.html`.

Bottom bar, icon only, ~~4 tabs from the right (D157): Today · Goals (target icon) · Dashboard · Week~~. Settings opens from the gear on Today.

| Screen | Was | Now |
|---|---|---|
| 01 Today | Today | Same, plus the goal strip and goal action tiles; no «پیکر» row; header without «هفته N از ۲۳» |
| 02 Goals | Progress tab | Goal cards, «+ هدف تازه», the «بعداً» list |
| 03 Goal detail | Progress content | Per goal (§5.6); for a fitness goal it also holds what Progress had |
| 04 Goal flow | Onboarding pattern | 8 steps, slides, back on every step, end layout (D147) |
| 05 Dashboard | Dashboard | Same grid and week rows; it scrolls back through the last 26 weeks **[DEFAULT]** instead of the program range |
| 06 Week | Friday review and plan | The weekly review (§5.7); special days stay; snack notes become a free day note **[DEFAULT]** |
| 07 Monthly review | Check-in flow | Per goal chapter end (§5.8); the fitness goal's check-in runs inside it |
| 08 Breakdown editor | Workout plan editor pattern | Months → weeks → actions, with sheets like the move sheet |
| Workout session, plan editor | Core screens | Only inside a fitness goal (§5.11) |
| Settings, Tasks, Task editor, Reminders, Backup, Appearance, «رسیدن یادآورها» | Same | Same; the task editor gains a «هدف» row (§6.4); Appearance loses the character switch |

**Today, top to bottom:** header (weekday, Jalali date · Gregorian, leaf, gear) → ring and week dots → **goal strip** → part-of-day pages with tiles → «هر وقت» → «+ کار جدید». One card at a time above the tiles, as now (goal cards join the queue, §5.9).

## 5. Goals

### 5.1 The model

```
Goal            → whole goal (any length; "short" = 6 weeks or less, D153)
 └ Chapter      → one per Jalali month of the goal; holds the monthly milestone (long goals only)
    └ Week      → Saturday–Friday; holds the weekly target as a range
       └ Action → a task linked to the goal; repeats; has a cue, a minimum and an if-then backup
```

**Goal fields:**
- Title, approach-framed («تا نوروز ۵ کیلومتر بی‌وقفه بدوم», not «دیگه تنبلی نکنم»).
- Why: one line («چرا این برای خود تو مهم است؟»). Optional.
- Type (sets the default lead metric and visuals): **نتیجه‌ی قابل اندازه‌گیری** (measurable outcome) · **مهارت و یادگیری** (skill) · **پروژه** (milestones) · **عادت** (consistency).
- Start date (default today) and finish date. Chips: «۱ ماه» · «۳ ماه» · «۶ ماه» · «تا نوروز» · «یه روز…» (the Jalali calendar).
- Lead metric: the behavior you control. Default: sessions, counted from the action's ticks. Or an amount with a unit (pages, minutes, pitches), entered by long-press.
- Lag metric (optional): the outcome, a number with a unit, a start value, an optional target, and «بیشتر/کمتر بهتره». Entered weekly or monthly.
- Obstacle and its if-then plan (WOOP). Optional.
- Flex days per week: 0, 1 or 2, default **1** **[DEFAULT]**.
- Icon (the 11 task icons) and order.
- State: **فعال** (active) · **بعداً** (parked: a title, no plan) · **استراحت** (snoozed for the rest of this month, R§4 YNAB) · **رسیدم** (done) · archived.
- Template: none or fitness (§5.11).

**Chapter (monthly milestone):** a statement («در ماه آخر چه کاری می‌کنی؟»), «تموم که شد یعنی…» (the evidence), an optional monthly range for the lead metric, and «هنوز مطمئن نیستم» as an allowed value from month 2 on. Chapters follow Jalali months; a first month shorter than 2 weeks joins the next, as Mehr–Aban did (D51).

**Week target:** the behavior («جلسه‌ی ۲۵ دقیقه‌ای»), a low and a high («۳–۵»), and the linked actions. Low = "even on a bad week", high = "a good week". Reaching the low counts as **on plan**.

**Action:** a task with verb + object + minimum («۵ صفحه بخونم، حداقل»), a part of the day, a cue (the anchor «بعد از…», e.g. «بعد از شام، پشت میز»), an if-then backup («اگه خسته بودم: ۱۰ دقیقه فلش‌کارت»), a repeat, and optionally an alarm at the cue time.

### 5.2 Goal types

| Type | Lead metric default | Main visual | Lag |
|---|---|---|---|
| Measurable outcome | Sessions/week | Weekly band | The outcome line, monthly, rolling average; pace to the target |
| Skill / learning | Focused sessions/week | Weekly band | Optional monthly test score; early months use learning milestones («۳ تا نقطه‌ضعفم رو پیدا کنم») (R§3.1) |
| Project | Milestones done | Milestone checklist «۳ از ۸» | None |
| Habit | Days done/week | Weekly band | None; the routine strength (§6.6) shows on the detail |

Every type also gets the weekly 1–5 «هدف چقدر جلو رفت؟» and the artifact gallery (photos, recordings, files, stored privately). A goal that is hard to measure leans on these.

### 5.3 Setting the goal (G1)

Eight steps on the onboarding pattern (slides, back on every step, main button at the bottom). Target: a minimum plan in under 6 minutes (R§5.2, D155).

| Step | Asks | Notes |
|---|---|---|
| ① هدف | «تا [تاریخ پایان] دوست داری چی واقعیت شده باشه؟» + the finish date chips | Avoidance wording (starts with «دیگه… نکنم»، «ترک»، «کمتر») gets «می‌شه اینو به شکل کاری که انجام می‌دی بنویسیم؟» with «عوض کن» / «همین خوبه». «فعلاً بعداً» saves it as a parked idea here. |
| ② چرا | «چرا این برای خود تو مهمه؟» one line | «رد شو» allowed. Shown again at chapter ends and the finish. |
| ③ مانع | «۱۰ ثانیه بهترین نتیجه رو تصور کن. مهم‌ترین چیزی که توی خودت ممکنه مانعت بشه چیه؟» | Chips: «حوصله‌ام سر بره»، «تغییر رو زود نبینم»، «رشته از دستم در بره»، «روالم به هم بریزه», or your own. Then «اگه پیش اومد، اون‌وقت…». Skippable. |
| ④ نوع و معیار | The type, then «کدوم کار هفتگی بیشترین اثر رو روی این هدف داره؟» | 3 example chips for the type (R§5.2 worked examples: fitness, study, income). The fitness chip offers the template (§5.11). |
| ⑤ از آخر به اول | «توی ماه آخر چی کار می‌کنی یا چی بلدی؟» then «و ماه قبلش؟» down to month 1 | Only month 1 is required; later months take «هنوز مطمئن نیستم». Skipped for short goals. |
| ⑥ این هفته | «یه بازه‌ی واقعی برای این هفته چیه؟ کمینه: حتی توی یه هفته‌ی بد. بیشینه: توی یه هفته‌ی خوب.» | Two steppers, pre-filled from step ④'s example. |
| ⑦ کار روزانه | «بعد از اینکه ___، ___ رو انجام می‌دم.» then «اگه ___ پیش اومد، اون‌وقت ___ (نسخه‌ی کوچیک‌تر).» | Part of the day from the cue; optional alarm. Then the rehearsal: «یه بار بلند بخونش، بعد بزن.» |
| ⑧ خلاصه | «یکی دو روز انعطاف در هفته انتخاب کن. بخشی از برنامه‌ان، نه تقلب.» and the summary card | «این برنامه‌ی توئه، به قلم خودت.» Save → back to the Goals tab with the new card. |

Quality checks (§5.5) run on steps ①, ⑥ and ⑦ and on save, one card at a time.

### 5.4 Breaking the goal down (G2)

- **Three planning levels plus actions**, never a fourth level of daily goals. Actions repeat; they are not new to-dos every day.
- **Backward first:** the flow plans from the last month to month 1. Later months stay «هنوز مطمئن نیستم» until their chapter review.
- **Fixed minimum, costly reserve:** the action has a minimum (the backup is the smaller version); flex days are the reserve (R§3.2, Sharif & Shu).
- **The breakdown editor** (goal detail → «برنامه»): chapter rows, each opening a sheet (statement, evidence, range); the current week's target; the actions (each opens the task editor with the goal fields). Same visual pattern as the workout plan editor.
- **Editing is normal.** Changing a range or swapping an action never recolors past weeks (frozen history, §6.1).

### 5.5 Quality checks (G3)

Simple local rules (R§5.3). At most one suggestion at a time, as a soft gray card with «عوض کن» / «همین خوبه». Keeping it is always fine, and a dismissed check doesn't come back for that field.

| Check | Rule | Wording |
|---|---|---|
| Vague verb | «بهتر بشم»، «کار کنم روی»، «سعی کنم» with no number or object | «می‌شه دیدنی‌ترش کنیم؟ کسی تو رو در حال انجام چه کاری می‌بینه؟» |
| Too big for a day | A daily minimum over 60 min, or more than 3 goal actions due on one day | «این بلندپروازانه‌ست. نسخه‌ای که یه روز خسته هم بشه انجامش داد چیه؟» |
| Too many | More than 3 weekly targets started this week, or more than 1 new daily action started this week (across all goals, D150) | «شروع کارهای کمتر معمولاً ماندگارتره. یکی رو برای ماه بعد بذاریم؟» |
| Outcome-only week | The week target is an outcome (kg, toman, score) with no behavior | «نتیجه گاهی دیرتر از تلاش می‌رسه. این هفته چی کار می‌کنی که بهش برسی؟» |
| No cue | An action with no «بعد از…» and no time | «این کار کجای روزت جا می‌گیره؟» |
| Avoidance | Starts with «دیگه… نکنم»، «ترک»، «کمتر» | «به‌جاش چی کار می‌کنی؟» |
| Planning fallacy | Every month of a long goal is filled with no lighter month | «برنامه‌ها معمولاً برای همه طولانی‌تر از انتظار می‌شن. یه هفته‌ی ذخیره اضافه کنیم؟» |
| Range too wide or narrow | High ≥ 3 × low, or low = high | «یه کم تنگ‌ترش کن که هفته‌ی خوب واقعاً حس هفته‌ی خوب بده.» |

### 5.6 Monitoring progress (G4–G6)

**Logging.** Ticking a goal action logs its minimum (one session). Long-press opens a small sheet: amount (for an amount metric) and a one-line note. The tick motion and haptic are the task's (D58, D76). Ticking the backup version counts the same.

**On Today (G6).**
- The **goal strip** under the ring: one small card per active goal with something due this week, swiped sideways: icon, title, a mini chapter path, «۲ از ۳–۵». Tap → goal detail. Snoozed and parked goals don't show.
- Goal actions sit in their part of the day as tiles with the goal's icon as a small badge and the cue as their second line («بعد از شام»).
- A fitness goal's workout action keeps the «تمرین امروز» card (D126).

**The Goals tab (G4).** Cards in the user's order: icon, title, dates, the chapter path, this week's band, and one neutral line («این هفته توی برنامه‌ای» when the low is reached, otherwise the count only). Below: «+ هدف تازه» and the folded «بعداً» list. Done goals are in «رسیده‌ها» at the end.

**The goal detail (G5).**
- Header: title, the why line, «هفته‌ی ۳ از ۱۲ · فصل آذر», the finish date or «N روز تا پایان» in the last chapter.
- **Chapter path:** a horizontal path with a station per week. In RTL it starts at the right edge and fills toward the finish flag on the left (time flows right → left). Flex weeks and snoozed weeks show as soft indigo segments, never as gaps of failure.
- **Weekly bands:** one row per week, a light fill between low and high, a dot per session. Hitting the low reads as on plan.
- **Outcome line** (when there's a lag metric): monthly points, a 4-week rolling average, numeric charts LTR like finance apps (R§5.6) **[DEFAULT, test with the user]**, and the line «نتیجه پله‌پله میاد، نه خطی.» With a target, a pace line «جایی که الان باید باشی» (Monzo) and a projected finish (Strides). Never a verdict word.
- **Milestones:** the chapter statements as a checklist; reached ones in the accent with the date.
- **1–5 line:** the weekly «هدف چقدر جلو رفت؟», as dots.
- **Artifacts:** photos, recordings and files added at chapter reviews, as a before/after gallery.
- **Letters:** sealed letters for this goal (D72 rules, per goal).
- «برنامه» opens the breakdown editor; «استراحت این ماه», «رسیدم», «بایگانی» sit in the overflow menu.

**Framing switch (R§3.2, R§5.6).** In the first half of each chapter, and the first half of the whole goal, progress reads «تا اینجا: ۷ جلسه». After the midpoint it reads «۳ تا پایان فصل». This counters the mid-goal slump.

### 5.7 The weekly review (G7)

The Friday ritual becomes the weekly review. Default Friday evening at the ritual time (user-adjustable), 5–7 minutes, with Saturday as the catch-up day as now (D140). A Today card «مرور هفته · ۵ دقیقه» while it's open.

1. **Look back (auto-filled):** the week's squares and wins, as now. Then one card per active goal: sessions logged against the range, flex days used, the dots, and the 1–5 «هدف چقدر جلو رفت؟» inline on the card.
2. **Four questions, once for the whole week** (D150), one line each, all skippable: «چی خوب پیش رفت؟» · «چی مانع شد؟» · «چی رو متفاوت امتحان می‌کنم؟» · and the 1–5s from step 1.
3. **Plan next week**, per goal: «مثل هفته‌ی قبل» (one tap, the default), change the range, or swap the action. Special days and the day note as now.
4. **Close:** re-read the if-then plans, one tap. The weekly backup runs.

**Short path:** «همه مثل هفته‌ی قبل» right under the look back finishes in about a minute (D62).

**Midweek check (optional, Tuesday or Wednesday):** one Today card per goal, only when the goal had no log for 2 days or the week's pace is under the low: «هفته‌ی «زبان» چطور پیش می‌ره؟» with «خوبه» · «یه کم سخته» · «باید عوضش کنم». The last two open the week target sheet.

### 5.8 The monthly chapter review (G8)

At each chapter's end (from its last Friday, or the first open in the new chapter), about 10 minutes, one per long goal:
1. The chapter path and the «done so far» numbers.
2. A before/after artifact (add one now, compare with the first).
3. Update the lag metric.
4. WOOP refresh for next month (the outcome, the obstacle, the if-then).
5. Write or edit the next milestone (this is where «هنوز مطمئن نیستم» gets filled).
6. Optionally a letter to the end-of-goal self.

A **re-commitment** is built in at the goal's halfway chapter: the halfway recap (§5.10) and a WOOP refresh, because motivation dips around weeks 6–12 (R§3.4).

### 5.9 Misses and re-planning (G9)

Rules (R§5.7), in order:
1. **One missed day:** nothing changes except a gray gap. No message.
2. **A flex day is left:** a missed due day is covered at week end and shows as a soft «انعطاف» dot. The review says «یه روز انعطاف استفاده شد؛ برای همین بود.»
3. **Two missed days in a row** on a goal: one gentle Today card, dismissible, at most once a week per goal: «خوش برگشتی. امروز نسخه‌ی کوچیکش؟» Yes ticks the backup version as today's plan.
4. **A week under the low:** the review asks «برنامه بزرگ بود یا هفته غیرعادی بود؟» with «هفته‌ی غیرعادی، برنامه بمونه» · «بازه رو کوچیک کن» · «زمان یا نشونه رو عوض کن».
5. **Two weeks in a row under the low:** a re-plan offer, never a reset: «بیا برنامه رو با زندگی فعلیت جور کنیم.» It suggests halving the range or swapping the action.
6. **Seven days or more without opening the app:** one «خوش برگشتی» screen, no summary of misses. «از همین‌جا ادامه بده» (keeps the plan) or «یه هفته‌ی سبک» (minimums only this week).
7. **Never:** red, crosses, «شکست خوردی», counts of missed days, or resetting chapter progress.

After a rough week the review offers, once, «به دوستی که چنین هفته‌ای داشته چی می‌گفتی؟» (self-compassion, R§3.5).

Wording frames misses as planning or context problems that can be fixed, never as low willpower.

### 5.10 Moments (G10)

- **Size hierarchy:** a tick = haptic + check morph (≤ 300 ms); a week reaching the low = a small inline note on the goal card; a chapter done = a full-screen moment («فصل ۲ تموم شد»), the numbers in «done so far» framing and the user's why line; the finish = the letter opening and the before/after artifacts.
- **Informational, not generic:** «۴۶ جلسه مطالعه؛ آزمون آزمایشی از ۵.۵ به ۶ رسید.»
- **Identity as earned evidence,** only at chapter ends: «این ماه ۱۸ روز مطالعه کردی. این یعنی آدمی که یاد می‌گیره.» Never an upfront «تو الان یه دونده‌ای».
- **Unexpected small acknowledgements,** rare: «اولین بار که از نسخه‌ی کوچیک‌تر استفاده کردی.» No variable-ratio rewards.
- **Halfway recap** for goals of 8 weeks or more, and a **finish recap** for long goals: the halfway and finish story patterns (D141, D145), per goal.
- **After the finish:** «بعدش چی؟»: «ادامه» (the goal is marked done; its actions stay as plain routines) · «هدف تازه» (the flow opens with this goal as a base) · «تموم» (actions archived).

### 5.11 The fitness template (G11, D152)

Choosing «تناسب اندام» in step ④ pre-fills:
- type habit + measurable, lead metric «تمرین در هفته», weight and the 5 tape sites as lag metrics, monthly 3-photo artifacts;
- a daily action «تمرین» linked to the workout plan.

It also turns on, for that goal only: the workout plan editor, the session, ladders and the next-rung offer, records, lifetime totals, strength tests with the guess, and the check-in with the ghost camera and the reveal. The check-in runs inside the monthly chapter review. These keep their v0.3 rules (git history: SPEC v0.3 §6.4, §6.7; D113–D122, D138–D139). Only one fitness goal at a time **[DEFAULT]**.

## 6. To-do rules (unchanged unless noted)

### 6.1 Which day an action counts for
- The day rolls over at **04:00**.
- **Edit window:** tick or untick today and yesterday only. Older days are read-only.
- **Before the app:** days before `installDate` show as «قبل از اپ», never as gaps.
- **Frozen history:** a day's score is saved and frozen once the day leaves the edit window (D37). The same goes for a goal's week: editing a target never recolors past weeks.

### 6.2 Day score and grid colors
- Due items = active routines scheduled for the day + "every N days" items that first became due that day. Goal actions count like any task.
- Level: 0 gray (nothing done) · 1 anything done or the floor · 2 ≥ 50% · 3 ≥ 80% · 4 = 100%.
- **Special day** (sick, travel, off): its own color, excluded from scores, at most 2 a week. A special day also counts as a flex day for every goal that week **[DEFAULT]**.
- **No streaks.** "Showed up X of the last 7 days" is the closest thing.

### 6.3 "Every N days from the last time" items
Unchanged: `nextDue = lastDoneDate + N`; a late item counts only on the day it first became due.

### 6.4 Goal fields in the task editor
The full form gains a «هدف» row (none or a goal). With a goal chosen it also shows «حداقل» (the minimum) and «اگه نشد…» (the backup). The simple sheet stays as it is (D128).

### 6.5 Routine strength and quieter reminders
Unchanged (D57, D144). The habit type's detail shows a strength line in words, never a number.

## 7. Reminders and notifications

Unchanged from v0.3 §7 (block notifications, follow-ups for «مهم», task alarms, no catch-up, no guilt, the absence rule, exact alarms, reminder health), plus:
- A goal action joins its part's block notification with its cue as the anchor («۵ صفحه · بعد از شام»). With its own alarm it rings at the cue time. At most one reminder per action per day (R§6).
- The weekly review reminder is the ritual reminder (Friday, ritual time).
- Chapter reviews, the midweek check and miss cards are Today cards only. No notifications.
- Notifications never mention a goal being behind.

## 8. Backup and data safety

Unchanged: everything local; a weekly zip (data JSON, a raw database copy, photos and artifacts) into the chosen folder that Syncthing syncs; the last 8 kept; restore with a preview and a safety backup first. The JSON format (`docs/BACKUP_FORMAT.md`) gains the goal tables. The usage log stays local (D59) and also records goal events (goal created, step reached in the flow, review done and its duration, re-plan accepted) for the later experiment plan.

## 9. Data model (conceptual)

New and changed tables; everything else as in v0.3 and `ARCHITECTURE.md` §8.

```
Goal            id, title, why?, type(OUTCOME|SKILL|PROJECT|HABIT), icon, startDate, finishDate,
                state(ACTIVE|PARKED|SNOOZED|DONE|ARCHIVED), snoozedUntil?, flexPerWeek,
                obstacle?, obstaclePlan?, leadName, leadUnit?, leadKind(TICKS|AMOUNT),
                lagMetricId?, lagTarget?, template(NONE|FITNESS), sortOrder, createdAt
Chapter         id, goalId, index, startDate, endDate, statement?, evidence?,
                rangeLow?, rangeHigh?, unsure, reachedAt?
WeekTarget      goalId, weekStart(Sat), behavior, low, high      -- one row per goal and week, copied forward
GoalWeek        goalId, weekStart, rating(1..5)?, flexUsed, frozenCount
WeekReview      weekStart, wentWell?, gotInTheWay?, tryDifferently?, durationSec   -- was WeekPlan
Artifact        id, goalId, date, kind(PHOTO|AUDIO|FILE), relativePath, note?

Routine         + goalId?, minimumText?, backupText?
Completion      + amount?, note?, usedBackup
Metric          + goalId?                                       -- lag metrics are metrics
Letter          + goalId
Workout         + goalId                                        -- the fitness goal's plan
PhotoSet        + goalId
Recap           + goalId
DayNote         date, text                                      -- was SnackNote
```

Derived values (week count vs range, flex coverage, framing, pace, projected finish, chapter of a date, quality checks) are computed in `:domain` and tested there, like the day score.

## 10. Migration (D154)

One database migration, tested like the others:
1. Create a goal from the fitness template: start 9 Oct 2026, finish 20 Mar 2027, chapters by Jalali month as before, flex 1. Title and why are empty; a one-time Today card asks for them, pre-filled with the identity sentence.
2. Attach the workout plan, sessions, metrics, photo sets, letters and recaps to it.
3. Link the workout task to it as its daily action.
4. Every other task stays a plain to-do item. Completions, day snapshots, special days and backups stay.
5. Week plans become week reviews; snack notes become day notes.
6. Settings: drop the character switch; the program dates move to the goal.

A backup runs before the migration.

## 11. Localization

**English and Persian** (D174, replaces D87). Persian: RTL, Persian digits, Jalali first with Gregorian second. English: LTR, Latin digits, Gregorian first with Jalali second (a setting swaps them). The language follows the phone, with a switch in Settings (D176). Weeks start Saturday in both **[DEFAULT]**. Copy uses informal «تو». The research's Persian wording is a starting point and should be read aloud for tone before it ships (R§8).

## 12. Open items

Tracked in `docs/TASKS.md`.
