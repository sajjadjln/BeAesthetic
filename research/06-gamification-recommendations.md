# 06 · Gamification recommendations

_Research date: 2026-10-10. Based on `05`. These are **proposals**; nothing in `docs/` has been changed. Each one passes the sustainability checklist in `05` §4._

## The short version

BeAesthetic's current design already makes the **right big calls**: no streaks, no points, no punishment, a floor (minimum day), capped skips (special days), a weekly fresh start (Friday). The research backs every one of them.

What's missing is aimed at the two things the user said make them quit: **boredom** and **not seeing change**. The research points to three gaps:

1. **The middle of the program has nothing new in it.** Motivation dips around halfway (Bonezzi et al. 2011), and most nudges stop working within weeks (HeartSteps; megastudy). Halfway is **week 11–12, right at Yalda (21 Dec 2026)**.
2. **Proof arrives late in the loop.** PRs are shown in the Friday review and on Progress, not when they happen.
3. **The moment after a missed day isn't designed.** It's the single most valuable moment in the gym megastudy.

---

## Ranked shortlist

Ranking = expected effect on **doing the program through week 23** × confidence in the evidence ÷ effort. Scores are judgment, not measurement (method in `09`).

### 1. Program chapters and a visible finish line
- **What:** split the 23 weeks into **5 chapters by Jalali month** (Mehr–Aban, Azar, Dey, Bahman, Esfand). Each starts with a fresh-start moment on the 1st of the month; the monthly check-in is the chapter's proof point (as adopted in D51). Show one quiet line on Today: «هفته ۵ از ۲۳ · فصل آذر» and on Progress a program line toward «نوروز ۱۴۰۶».
- **Why:** shorter goals mean more "almost there" moments (goal gradient; Kivetz 2006) and more fresh starts (Dai 2014); breaking the long middle fights the halfway dip (Bonezzi 2011). Fabulous journeys and Duolingo's path use the same idea.
- **Adapted, not copied:** no levels or ranks; chapters are calendar facts the user already lives by (Jalali months), and they line up with the existing monthly check-ins (D28).
- **Effort:** S (a pure function in `:domain/time` + two text lines). Replaces backlog item "5-week phases".
- **Measure:** weekly show-up days in the 2 weeks after each chapter start vs the 2 weeks before.
- **Risk:** chapter "resets" could feel like starting over to someone doing well (Dai 2018). Mitigation: chapters **add**; they never reset any count.

### 2. Design the day after a miss: "comeback" as a win
- **What:** when a day at level 0 is followed by a day with any tick, that day is a **comeback**. On the first tick, Today shows one warm line («خوش برگشتی»), never naming the miss. The Friday review lists comebacks among wins («۲ بار برگشتی»). The existing welcome-back card (2+ days away) stays.
- **Why:** the top program in a 61,293-person megastudy rewarded **returning after a missed workout** (+27% visits). Being able to repair a break weakens the demotivating effect of seeing it (Silverman & Barasch 2023). "Missing once doesn't break habit formation" (Lally 2010).
- **Adapted:** no bonus points, no streak to protect. The reward is recognition.
- **Effort:** S (a rule over day snapshots; one string; a Friday-review row).
- **Measure:** comeback rate = share of level-0 days followed by a level ≥ 1 day; count of **two-day gaps** per month.
- **Risk:** could feel patronizing if shown too often. Show the line once per comeback, not on every tick.

### 3. Celebrate a PR the moment it happens
- **What:** in the workout session, when a logged set beats the previous best for that exercise, the set circle flashes the accent and shows «رکورد». Strength tests do the same.
- **Why:** competence predicts adherence (Teixeira 2012); immediate rewards beat delayed (Adams 2017); novices gain strength fast (~15% in 4 weeks in one study, Wilson 2023), so PRs will be frequent in weeks 1–8, exactly when the mirror shows nothing.
- **Adapted:** Strava/Strong-style PRs, but private and quiet, inside the existing one-tap logging.
- **Effort:** S (PR logic already planned in `:domain/workout`).
- **Measure:** PRs per week; self-rating after sessions (optional).
- **Risk:** PRs slow down after the beginner phase. Pair with chapter milestones (R9) so proof doesn't depend on PRs alone.

### 4. An anchor cue for each task ("after…")
- **What:** an optional short field in the task editor: «بعد از…» (after breakfast, after brushing, after the workout). Shown in the block notification next to the task name and in the task's long-press detail. Tiles stay 1–2 words.
- **Why:** implementation intentions (when-then plans) have a medium-to-large effect on goal attainment (d = 0.65; Gollwitzer & Sheeran 2006). **Event-based cues build automaticity; reminders alone don't** (Stawarz 2015). This is the main defense against **dependency** (Renfree 2016), so routines like skincare survive after week 23.
- **Adapted:** no new screen; one optional field, prefilled for the seed tasks.
- **Effort:** S (one column, one field, notification text).
- **Measure:** self-rated automaticity of 3 key routines at weeks 4, 12, 23 (a 4-question version of the Self-Report Behavioural Automaticity Index; ~30 s, done in the Friday ritual).
- **Risk:** none significant.

### 5. Days before the app aren't gaps
- **What:** the program started on 9 Oct; the app will arrive weeks later. Days before install show in a **neutral "before the app" style** (a dotted outline, not gray), and onboarding/baseline entry offers a 30-second **backfill** of workouts done since Day 0.
- **Why:** otherwise the first look at the Dashboard is **2–4 weeks of gray**, the exact "bad week makes me avoid looking" trigger (D6). Endowed progress: starting with progress on the card increases completion (Nunes & Drèze 2006).
- **Effort:** S (one flag: `installDate` in settings; one cell style; backfill reuses the edit flow once).
- **Measure:** does the user open the Dashboard in the first week? (Self-report or a local counter.)
- **Risk:** backfill invites guessing. Only workouts and strength baseline, not every routine.

### 6. A routine strength score, used behind the scenes
- **What:** compute Loop's strength score per task (`0.5^(sqrt(freq)/13)` decay, ~13-day half-life for daily tasks) in `:domain`. Use it to: (a) pick **"best routines"** for the Friday review (wins only, as designed); (b) after ~week 8, **offer to fade a reminder** for a routine that has stayed above a high threshold for 4+ weeks («مسواک شب جا افتاده. زنگش خاموش شود؟»). Never show per-task scores on Today.
- **Why:** a forgiving consistency measure (Loop) beats a streak; fading reminders for established routines reduces dependency and notification load (Renfree 2016; HeartSteps habituation).
- **Effort:** S for the score; M for the fade offer.
- **Measure:** routines still done ≥ 80% of days two weeks after their reminder is faded.
- **Risk:** fading too early. Make it an offer, reversible, never automatic.

### 7. Small, satisfying completion feedback
- **What:** a soft haptic + short accent fill on tick; a slightly bigger moment when a **part of the day** completes and when the **day hits 100%**. Rotating, specific copy for wins in the Friday review.
- **Why:** the design is calm and minimal and risks feeling cold; Finch shows warmth drives attachment. Celebration helps in the moment, though it doesn't build habit by itself (Stawarz 2015).
- **Effort:** S. **Measure:** subjective ("does ticking feel good?") after week 2.
- **Risk:** noise. Cap it to these three moments.

### 8. Halfway and finish recaps
- **What:** at **Yalda (week 11, ~21 Dec)** a private recap: days shown up so far, PRs, strength deltas, Day 0 vs latest photo pair, the user's identity sentence. At **29 Esfand** the full program recap. In the app only, no image export (D63: no sharing).
- **Why:** recaps turn data into a story at a meaningful moment (Strava's Year in Sport); halfway is the motivational low point (Bonezzi 2011); physically recorded progress strengthens monitoring effects (Harkin 2016).
- **Effort:** M. **Timing constraint:** must ship **before 19 Dec 2026** to land at halfway. If the build schedule can't make it, ship a simpler version (numbers only).
- **Measure:** show-up days in the 3 weeks after Yalda vs the 3 before.
- **Risk:** a weak recap (few PRs) could deflate. Lead with days shown up and strength deltas, which will be positive.

### 9. A few real-world milestones (experiment)
- **What:** 5–8 milestones the user picks during setup, tied to the body, not the app: first 10 strict pull-ups, 2-minute plank, −3 cm waist, 20 workouts logged. Shown when reached and in the recaps. No badge wall.
- **Why:** subgoals help in long goals (R1 evidence); real-world milestones feed competence, not points.
- **Effort:** S–M. **Status:** experiment; overlaps with backlog "Goals with progress bars".

### 10. Show special days as reserves (experiment)
- **What:** in the Friday plan, show special days as «۲ روز ذخیره» rather than a limit.
- **Why:** emergency-reserve framing raised persistence after failure (Sharif & Shu), and the label made people reluctant to spend them.
- **Effort:** S (copy only). **Status:** experiment (A/B by alternating months is impractical with n = 1; just try it and note how it feels).

---

## Recommendation matrix (all mechanics)

| Mechanic | Verdict | Where in BeAesthetic | Effort |
|---|---|---|---|
| Points / XP | **Avoid** | Day level is the only score | — |
| Levels / ranks | **Avoid** → use chapters | R1 | — |
| Streaks | **Avoid** (banned) | Rolling "X of last 7" | — |
| Streak recovery / flexibility | **Implement** (mostly done) | Minimum day, special days, edit window, R2 | S |
| Badges | **Experiment** (real milestones only) | R9 | S–M |
| Daily quests / weekly challenges | **Defer** | Only if boredom shows | M |
| Long-term path | **Implement** | R1, R8 | S–M |
| Leaderboards | **Avoid** | — | — |
| Social accountability | **Rejected by the user (D63: no sharing)** | Manual weekly share image to one friend (conflicts with SPEC §3.3) | S |
| Currencies / unlockables | **Avoid** | — | — |
| Avatars | **Avoid**; identity sentence **experiment** | Friday ritual, recaps | S |
| Progress indicators | **Implement** (done) + finish line + R5 | Today, Dashboard, Progress | S |
| Variable rewards | **Avoid** | Honest copy variety only | — |
| Personal bests | **Implement** | R3 | S |
| Adaptive difficulty | **Keep as designed** (suggest, don't change) | SPEC §6.4 | — |
| Narrative | **Defer fiction**; framing via R1 | — | — |
| Celebration | **Implement, small** | R7 | S |
| Loss aversion / commitment | **Avoid** loss; keep Friday plan | — | — |
| Reminders | **Implement as designed** + R4 + R6 fade | Slices 2–3 | S–M |
| Seasonal events | **Implement as calendar moments** | R1, R8 | S–M |
| Minimum day | **Keep** | Designed | — |
| Emergency reserves | **Keep** + R10 | Designed | S |
| Implementation intentions | **Implement** | R4 | S |
| Strength score (internal) | **Implement** | R6 | S–M |
| Recaps | **Implement** | R8 | M |

## Where each fits in the build order (`ARCHITECTURE.md` §14)

| Slice | Add |
|---|---|
| 1 Skeleton + Today | R7 tick feedback; the «هفته N از ۲۳» line (R1); R2 comeback line on Today |
| 2 Reminders | R4 anchor text in notifications (field can default empty until slice 3) |
| 3 Tasks editor | R4 field |
| 4 Workout | R3 in-session PR |
| 5 Dashboard | R5 pre-app style |
| 7 Progress / check-in | R1 program line; R5 backfill; R9 milestones (if chosen) |
| 8 Friday | R2 comebacks in wins; R6 best routines; R10 reserves copy |
| New, before 19 Dec | R8 halfway recap |
| Later (≥ week 8) | R6 reminder fade offer |

## What to avoid copying (and why)

| Don't copy | From | Why |
|---|---|---|
| Streak counters, streak-at-top | Duolingo, Snapchat, «عادت‌ساز» | Broken streaks demotivate independent of behavior; violates product rules |
| Restart-on-miss | 75 Hard | All-or-nothing; experts warn it undermines lasting change |
| HP loss / shared punishment | Habitica | Invites cheating and shrinking goals |
| Pets and cosmetic economies | Finch | Big content cost; pet becomes a chore; needs monetization |
| Leagues and leaderboards | Duolingo, Strava | No peers; shame for low ranks |
| Variable rewards | Many | Attention-capture pattern; erodes trust |
| More notifications to lift engagement | Duolingo | Duolingo optimizes DAU; BeAesthetic optimizes doing the program |
