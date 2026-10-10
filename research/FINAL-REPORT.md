# BeAesthetic research: final report

_Research date: 2026-10-10 (Week 1 of the program). Files `00`–`10` hold the detail; `SOURCES.md` the source log._

**Limitation up front:** this environment blocked direct page access. Evidence comes from web-search excerpts of papers, company posts, reviews and store listings. No competitor app screens were inspected first-hand. Loop Habit Tracker's source code was read directly. Prices and ratings are as reported by secondary sources and may be out of date.

---

## Executive summary

BeAesthetic is a personal, offline Android coach for one person's 23-week body transformation (9 Oct 2026 → 20 Mar 2027). It has **one user, no business model and no code yet**. So "winning" here means **the user is still doing the program in week 23, and some routines survive after it**. A Cafe Bazaar release is a possibility, not a plan.

**The design is already right on the things competitors get wrong.** No streaks, no punishment, a minimum-day floor, capped special days, local data, verified backups and a reminder health check each have research behind them. Competitors keep failing on exactly these points: punishing misses (Habitica, 75 Hard, a Persian habit app that resets after one miss), surprise billing (Finch, Fabulous) and data loss (Strong, Finch).

**The risks are time and the long middle, not the screens:**
1. The program is running **without the app** and the build has 9 slices.
2. Nothing new happens between week 5 and week 23, while motivation predictably dips around halfway (**week 11, Yalda**) and most nudges stop working within weeks.
3. Nothing defines **what happens after 20 March**, when the reminders the routines depend on would go away.

**Top recommendations** (all small, all inside the planned slices):
1. Put **reminders in the first installable build**.
2. Split the program into **chapters by Jalali month**, with a **halfway recap at Yalda** (ship by 19 Dec) and the finish framed as **the eve of Nowruz**.
3. **Celebrate PRs in the moment**, inside the workout session.
4. Design **the day after a miss**: recognize comebacks, never name the miss.
5. Add an **«بعد از…» anchor** to tasks so routines become automatic rather than reminder-dependent; later, **fade reminders** for routines that have stuck.
6. Don't let the Dashboard open on **weeks of gray** from before the app existed.
7. Decide now what **"after the finish line"** means.

---

## Answers to the 12 questions

### 1. What market are we competing in?
The overlap of **habit/routine trackers, workout loggers and body-progress apps**, in health & fitness, a category that keeps only **~3–4% of users at day 30** (AppsFlyer, Braze, UXCam). For a Bazaar release: Persian body-transformation / self-improvement, where supply found in searches is **translated global apps or Persian apps last updated in 2014–2015**, plus simple Persian habit apps. No market size was found and none is estimated.

### 2. Who are our most important competitors?
For the daily-routine job: **Loop** (closest technical twin: free, offline, a forgiving score), **Finch** (tone leader), **HabitNow**, **Fabulous**, **Habitica**, and on Bazaar **«عادت‌ساز»**. For training and proof: **Strong**, **Hevy**, **FitNotes**, progress-photo apps (**Metamorph, PhotoJourney**). The real competitor is **quitting**, followed by **paper or a coach's PDF**.

### 3. What do the best competitors do exceptionally well?
- **Fast logging** with last session's numbers at hand (Strong, FitNotes).
- **Warmth** at the moment of completion (Finch).
- **A forgiving consistency score** instead of a streak (Loop: 13-day half-life).
- **Renewal over months**: journeys (Fabulous), recaps at meaningful moments (Strava's Year in Sport), streak protection that brings people back (Duolingo Weekend Amulet: +4% returning a week later).
- **Planned rest without failure** (Apple's ring pause, Todoist vacation mode).
- **Ghost-overlay cameras** for comparable photos.

### 4. What are their weaknesses and customer complaints?
Recurring: **surprise billing** (Finch, Fabulous), **data loss** (Strong "lost all my workouts after an update", Finch "pet data corrupted"), **punishment** that invites cheating (Habitica wiki) or restarts (75 Hard), **paywalls and caps**, notifications that don't arrive on Xiaomi, **novelty fading** (Finch's pet "becomes a chore"). Survey: people stop health apps because of **data entry (44%), losing interest (40%), hidden costs (36%)**.

### 5. What do users appear to value most?
Speed, safety of their data, a non-judgmental tone, visible progress, and freedom to miss a day. **[Inference from praise and complaint patterns; no direct segment data]**

### 6. Where is there an opportunity to differentiate?
**"A hard program with kind rules, that works offline."** Nothing found combines routines + training + body proof, shame-free, offline, Persian-first. In Iran, offline-first is a survival trait: the internet was cut from 8 Jan 2026 and about **120 days** of disrupted access were reported by May 2026; FCM push was disrupted in 2022.

### 7. Which marketing channels should we prioritize?
**None now.** If a release happens: (1) Bazaar listing (ASO), (2) small Iranian fitness creators who already sell plans by DM, (3) practical Persian content (e.g. "making reminders work on Xiaomi"). Skip paid ads and referral programs. Lead message: «یه روز بد، برنامه‌ات رو خراب نمی‌کنه» + «بدون اینترنت کار می‌کند». Price: free or one-time, never a subscription. The biggest release blocker isn't marketing: it's that the app assumes users bring their own program.

### 8. Which gamification mechanics should we use, and why?
Ranked shortlist (detail in `06`):

| # | Mechanic | Why |
|---|---|---|
| 1 | **Chapters by Jalali month + finish line at Nowruz** | Motivation dips at halfway (Bonezzi 2011); fresh starts at landmarks (Dai 2014); goal gradient near the end (Kivetz 2006) |
| 2 | **Comeback recognition** | Top program in a 61,293-person gym megastudy rewarded returning after a miss (+27%); repairable breaks hurt less (Silverman & Barasch 2023) |
| 3 | **In-session PR moment** | Competence predicts adherence (Teixeira 2012); immediate > delayed rewards (Adams 2017); novices gain ~15% strength in 4 weeks |
| 4 | **Anchor cues per task** | Implementation intentions d = 0.65; event cues build automaticity, reminders alone don't (Stawarz 2015) |
| 5 | **Pre-app days not gray** | Endowed progress (Nunes & Drèze 2006); avoids the "avoid looking" trigger |
| 6 | **Internal routine strength score** (Loop formula) → best routines, reminder fading | Forgiving consistency measure; reduces app dependency (Renfree 2016) |
| 7 | **Small completion feedback** | Warmth in the moment (Finch); keep it to 3 moments |
| 8 | **Halfway (Yalda) and finish recaps** | Recap at meaningful moments (Strava); halfway is the low point |
| 9 | Real-world milestones (experiment) | Subgoals in long goals |
| 10 | Special days as «ذخیره» (experiment) | Emergency-reserve framing (Sharif & Shu) |

### 9. What should we improve in our product first?
In order: **F1** reminders first → **F4** pre-app days → **R1** chapter line → **R7** tick feedback → **R4** anchors → **R3** PR moment → **R2** comebacks → **R8** Yalda recap by 19 Dec → **F3** decide "after the finish line". All S-sized except the recap (M). See the action plan below.

### 10. What should we avoid copying?
Streak counters and streak-at-top (Duolingo, «عادت‌ساز»); restart-on-miss (75 Hard); HP loss and shared punishment (Habitica); pets and cosmetic economies (Finch); leaderboards and leagues; variable rewards; AI coaches and AI body scoring (needs foreign servers; looksmaxxing apps have a toxic reputation); subscriptions; more notifications to lift engagement.

### 11. What should we test in the next 30, 60 and 90 days?

| Window | Test (from `10`) |
|---|---|
| **30 days** (to ~9 Nov, week 5) | No experiments yet; build slices 1–2 and collect a **2–3 week baseline**. Photo consistency checklist before the 2nd photo set (~Fri 6 Nov). |
| **60 days** (to ~9 Dec, week 9) | **E1** anchor cues (multiple baseline across 4 routines) · **E4** rotating notification wording (ABAB) · **E5** in-session PR moment (ABAB) |
| **90 days** (to ~8 Jan, week 13) | **E3** chapter fresh starts (each month boundary) · **E8** Yalda recap (with the Dey boundary) · **E6** reminder fading from week 8 · **E7** short Friday ritual |

### 12. What additional data do we need?
- **The user's own baseline** (first 2–3 weeks): show-up days, weekly score, reminder health.
- **A local usage log** (F8, needs the user's OK): app opens, notification → first tick latency. Never leaves the phone.
- **SRBAI automaticity** for 3 routines at weeks 4, 12, 23.
- **Answers to open questions** in `TASKS.md` (bedtime, training time, check-in Friday rule) that shape reminder timing.
- **For a release only:** Bazaar search demand for Persian fitness/habit terms; current Bazaar reviews of competing apps (needs page access); whether Bazaar allows `USE_EXACT_ALARM`.

---

## Key findings ranked by importance

1. **Time is the biggest risk.** Every week without reminders is lost cueing during the ~66-day median habit window. **[Sourced + Fact]**
2. **The middle of the program will sag** (week 11 ≈ Yalda). The design has nothing there yet. **[Sourced]**
3. **Reminders build dependency**; without event cues and a plan for after 20 March, routines may stop with the app. **[Sourced]**
4. **The no-shame design is evidence-backed** and is the main differentiator. Keep every rule. **[Sourced]**
5. **Proof is too late in the loop**: PRs should land in the moment. **[Sourced + Inference]**
6. **The first Dashboard view could be weeks of gray.** **[Inference from the timeline]**
7. **Offline-first is a real advantage in Iran**, proven by the 2026 blackouts. **[Sourced]**
8. **Gamification helps modestly and fades** (g = 0.42 → 0.15 at follow-up); renewal beats any single mechanic. **[Sourced]**
9. **Persian supply is thin and dated**, so a release would stand out, but it would need starter programs and onboarding. **[Sourced + Inference]**
10. **No measurement exists** to tell whether changes work. **[Fact]**

## Competitor comparison (condensed; full matrix in `03`)

| | BeAesthetic | Loop | Finch | Habitica | «عادت‌ساز» | Strong / Hevy | Photo apps | 75 Hard |
|---|---|---|---|---|---|---|---|---|
| Routines | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ |
| Workout log | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| Body proof | ✅ | ❌ | ❌ | ❌ | ❌ | partial | ✅ | photos |
| Finite program | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Miss handling | Gray gap, floor, reserves | Decaying score | Gentle | **HP loss** | **Reset** | n/a | n/a | **Restart** |
| Offline / no account | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ | some | n/a |
| Persian / Jalali | ✅ | — | — | — | ✅ | — | — | — |
| Price | n/a | Free | Subscription | Freemium | 16k toman | Freemium / lifetime | Mostly free | Free |
| Main weakness | Unbuilt; mid-program boredom | Plain | Billing, depth | Punishment | Resets | Data loss (Strong) | Single-purpose | Harshness |

## Gamification recommendation matrix

| Implement | Experiment | Defer | Avoid |
|---|---|---|---|
| Chapters + finish line · comeback recognition · in-session PRs · anchor cues · progress line + pre-app cells · internal strength score · small celebration · Yalda + Nowruz recaps · minimum day (keep) · special days (keep) · reminders as designed | Real-world milestones · special days as reserves · identity sentence · weekly share image (needs a scope change) | Daily quests / weekly challenges · narrative layer | Points/XP · levels/ranks · streaks · leaderboards · currencies · avatars/pets · variable rewards · loss mechanics |

## Prioritized action plan

| # | Action | When | Effort |
|---|---|---|---|
| 1 | Reorder slices 1–2 so the first install has reminders | Now | — |
| 2 | Add `installDate`; pre-app cells; workout backfill at baseline | Slices 1, 5, 7 | S |
| 3 | «هفته N از ۲۳» + chapter line; tick haptic/animation | Slice 1 | S |
| 4 | Manifest check: no `INTERNET` permission | Slice 1 | S |
| 5 | Photo consistency checklist | Before Fri 6 Nov | S |
| 6 | «بعد از…» anchors in tasks and notifications | Slices 2–3 | S |
| 7 | In-session «رکورد» | Slice 4 | S |
| 8 | Comeback line; comebacks in Friday wins | Slices 1, 8 | S |
| 9 | Yalda halfway recap (numbers-only fallback) | **By 19 Dec** | M |
| 10 | Ghost-overlay camera | Before the Azar check-in (~11 Dec) | M |
| 11 | Decide "after the finish line"; build by ~week 18 | Decide by week 9 | S + M |
| 12 | Reminder fade offer | From week 8 | M |

## Experiment backlog (summary; full designs in `10`)

| ID | Test | Design | Primary metric |
|---|---|---|---|
| E1 | Anchor cues | Multiple baseline, 4 routines, 6 weeks | % due days done; SRBAI |
| E2 | Comeback recognition | ABAB, 3-week blocks | Comeback rate |
| E3 | Chapters as fresh starts | Before/after each month boundary | Show-up days/week |
| E4 | Rotating notification text | ABAB, 2-week blocks | Notification → tick latency |
| E5 | In-session PR moment | ABAB, 2-week blocks | Post-session rating |
| E6 | Reminder fading | Pre/post with 80% guardrail | Adherence after fading |
| E7 | Short Friday ritual | Pre/post, 4 + 4 weeks | Rituals done / weeks |
| E8 | Yalda recap | Landmark comparison | Show-up days/week |
| E9 | Reserves wording (optional) | Pre/post | Special-day use; rebound |
| E10 | Weekly share image (optional) | ABAB, 3-week blocks | Show-up days/week |

## Sources and research limitations

- **Sources:** ~120 links in `SOURCES.md`: peer-reviewed studies (habit formation, streaks, goal pursuit, gamification trials, motivation), company posts (Duolingo, Todoist, Android Developers), reviews and store listings, Iranian tech press.
- **Limitations:**
  - Direct page access was blocked; claims rely on search excerpts. Full texts, current store pages and app screens were not inspected.
  - Many competitor details come from aggregator or vendor blogs with commercial bias (marked "A" in the source log).
  - Effect sizes come from large populations of casual users; only the **direction** of effects is assumed to carry over to one motivated user.
  - Bazaar coverage is a sample, not a category scan.
  - No market size, share or revenue figure was found that I'd trust, and none is estimated.
- **To improve:** allow `play.google.com`, `cafebazaar.ir`, `pubmed.ncbi.nlm.nih.gov` and `dontkillmyapp.com` in the environment's network settings and re-run the competitor checks.
