# 08 · Product audit

_Research date: 2026-10-10. Audited: the approved spec, design and architecture (`docs/`), since no app exists. Findings are ordered by priority (impact × feasibility). "Complexity": S = hours to a day, M = a few days, L = a week or more, for one developer in this stack._

## Overall verdict

The design is **unusually well aligned with the evidence** on the things competitors get wrong: no streaks, a floor, capped skips, local data, verified backups, reminder health. Don't redesign it.

The real risks are not in the screens. They're in **time** and **the long middle**:

1. The program is already running **without the app**, and the build has 9 slices.
2. Nothing new happens between week 5 and week 23, while research says motivation dips around halfway and most nudges fade within weeks.
3. Nothing defines **what happens on 21 March 2027**, when the program ends and the reminders the habits depend on go away.

Below: 16 findings. Top 8 are worth doing; the rest are smaller or conditional.

---

## Priority 1: do these

### F1. Time to first value: get reminders onto the phone first
1. **Problem:** Week 1 started 10 Oct. The user said "every week without the app is a week without reminders" (`TASKS.md`). Slice 1 (skeleton + full Today) comes before slice 2 (reminders). Reminders are the value the user feels most.
2. **Evidence:** habit automaticity takes a median ~66 days (range 18–254) to form (Lally 2010), so every early week of cued repetition counts; the routines the user names as slipping (skincare, brushing) depend on cues.
3. **Reference:** none needed; this is sequencing.
4. **Why it works:** gets the highest-value loop (cue → tick) running weeks earlier.
5. **Adapted:** make slice 1 thinner (Today with tiles and ticking, no ring polish or week dots) and pull the reminder planner + `setAlarmClock` + block notifications into the **first** installable build. Polish Today in slice 2.
6. **Complexity:** none extra (re-ordering). 
7. **Risks:** a rougher first build. Dependency: the planner needs the task model, which slice 1 creates anyway.
8. **Measure:** date of the first week with a clean reminder health log.

### F2. Plan the middle: chapters by Jalali month and a halfway moment at Yalda
1. **Problem:** the only novelty mechanism is backlog item "5-week phases" (~week 5). The design has no landmarks between Day 0 and the finish.
2. **Evidence:** motivation is lowest around halfway (Bonezzi 2011); fresh starts at temporal landmarks raise goal behavior (Dai 2014); in a 61k-person megastudy only 8% of programs still worked after they ended (Milkman 2021); prompt effects fade to ~0 by day 28 (HeartSteps).
3. **Reference:** Fabulous journeys; Duolingo path; Strava's Year in Sport.
4. **Why for this user:** "quits when bored" and "no clear goal" are the stated weak points. Chapters give 5 near-term goals instead of one far one.
5. **Adapted:** chapters = Jalali months, with the monthly check-in (D28) as each chapter's proof point; Yalda (21 Dec, week 11) = halfway recap; 29 Esfand = finish recap, the eve of Nowruz. See `06` R1, R8.
6. **Complexity:** S for chapters; M for the recap.
7. **Risks:** the recap must ship **before 19 Dec** or it misses its moment. Make a numbers-only version the fallback.
8. **Measure:** show-up days in the 2–3 weeks after each landmark vs before.

### F3. Define "after the finish line"
1. **Problem:** SPEC and ARCHITECTURE stop at 20 March 2027 (`programEndDate` exists, behavior after it doesn't). If reminders just stop, the research predicts the routines stop with them.
2. **Evidence:** habit apps' reminders and streaks create **dependency**; users quit the app and drop the behavior (Renfree 2016). Effects of incentives and nudges fade when they end (Patel 2016; Milkman 2021).
3. **Reference:** Duolingo and Apple never "end"; Fabulous moves you to the next journey.
4. **Why:** the user's goals include lasting routines (skincare, teeth, nails), not only a 23-week body change.
5. **Adapted:** decide now, build later (around week 18): after the finish recap, offer **maintenance mode** (routines continue, workouts optional, chapters end) or **a new program** (new dates, same tasks). Meanwhile, from week ~8, fade reminders for established routines (F6).
6. **Complexity:** S to decide; M to build.
7. **Risks:** scope creep; keep it to "continue" vs "new dates".
8. **Measure:** routines done ≥ 80% of days in April 2027.

### F4. Don't open the Dashboard on weeks of gray
1. **Problem:** the app arrives weeks after Day 0. The first Dashboard view will show the gap since 9 Oct as gray, which is the "bad week makes me avoid looking" trigger (D6).
2. **Evidence:** endowed progress (Nunes & Drèze 2006); how a log displays breaks changes later behavior regardless of actual behavior (Silverman & Barasch 2023).
3. **Reference:** none direct; Apple shows "no data" rather than "failed" before you had the watch.
4. **Why:** the very first impression of the grid sets whether the user keeps looking at it.
5. **Adapted:** store `installDate`; days before it render as a dotted "before the app" cell (not gray); baseline entry offers a quick backfill of **workouts and strength tests** since Day 0. See `06` R5.
6. **Complexity:** S.
7. **Risks:** none significant.
8. **Measure:** the user opens the Dashboard in the first week.

### F5. Celebrate PRs when they happen
1. **Problem:** PRs appear in the Friday review and on Progress, days after the effort.
2. **Evidence:** immediate rewards beat delayed (Adams 2017); competence predicts adherence (Teixeira 2012); novice strength rises ~15% in 4 weeks in one study (Wilson 2023), so PRs will come often early.
3. **Reference:** Strava, Strong, Hevy PRs.
4. **Why:** "proof early" is design principle 4; this delivers proof in the moment.
5. **Adapted:** «رکورد» flash on the set circle; no sound, no confetti.
6. **Complexity:** S. 7. **Risks:** PR rate falls after the beginner phase (pair with F2). 8. **Measure:** PRs per week; how sessions feel.

### F6. Cue routines to events, and fade reminders when they've stuck
1. **Problem:** routines are cued by clock-time notifications only. That supports repetition but not automaticity.
2. **Evidence:** event-based cues increased automaticity, reminders didn't (Stawarz 2015); implementation intentions d = 0.65 (Gollwitzer & Sheeran 2006).
3. **Reference:** Atoms (time, place, action); Loop's strength score.
4. **Why:** builds habits that outlive the app (F3).
5. **Adapted:** optional «بعد از…» anchor per task, shown in notifications; Loop-style strength score in `:domain`; from week ~8, offer to silence the reminder of a routine that's been strong for 4+ weeks. See `06` R4, R6.
6. **Complexity:** S (anchor, score); M (fade offer).
7. **Risks:** fading too early; keep it an offer, reversible.
8. **Measure:** SRBAI automaticity (4 items, Gardner et al. 2012) for 3 routines at weeks 4, 12, 23; adherence after fading.

### F7. Design the day after a miss
1. **Problem:** a single missed day has no designed response. The welcome-back card only appears after 2+ days away.
2. **Evidence:** the top program in the gym megastudy rewarded returning after a missed workout (+27%); repairable breaks hurt less (Silverman & Barasch).
3. **Reference:** Duolingo streak repair; "never miss twice".
4. **Why:** the user's failure mode is a slide from one bad day into a bad week.
5. **Adapted:** «خوش برگشتی» on the first tick after a level-0 day; "comebacks" as a Friday win. See `06` R2.
6. **Complexity:** S. 7. **Risks:** patronizing if repeated; once per comeback. 8. **Measure:** comeback rate; two-day gaps per month.

### F8. A local usage log so experiments can be judged
1. **Problem:** "no analytics" is a rule, so there's no way to tell whether any of this works beyond the day snapshots.
2. **Evidence:** self-monitoring works better when physically recorded (Harkin 2016); the experiments in `10` need a few counts.
3. **Reference:** the architecture already has a local crash log with no SDK (§11).
4. **Why:** n = 1 experiments need data, and memory is biased.
5. **Adapted:** a tiny `event` table (app opened, screen viewed, notification tapped, notification → first tick latency), **never leaves the phone** except inside the user's own backup; viewable in the debug menu; deleted after 180 days. Not an SDK, not analytics in the product rule's sense. **Needs the user's OK.**
6. **Complexity:** S. 7. **Risks:** scope creep into dashboards; keep it to raw rows + a debug view. 8. **Measure:** n/a (enabler).

---

## Priority 2: worth doing, smaller

| # | Finding | Evidence / reference | Adaptation | Complexity | Measure |
|---|---|---|---|---|---|
| F9 | **Completion feels cold.** Calm design, no feedback moment. | Finch warmth; immediate feedback (Adams 2017) | Haptic + accent fill on tick; a moment for a finished part-of-day and a 100% day (`06` R7) | S | "Does ticking feel good?" after week 2 |
| F10 | **Photo comparability.** Ghost overlay is backlog (~week 4); 2nd set ≈ Fri 6 Nov (closest Friday to 17 Aban, if the D-default holds). | Photo apps make ghost overlay standard | Ship ghost camera before the 3rd set (~11 Dec); for the 2nd, add a one-screen consistency checklist (spot, light, distance, time, clothing) | M | Photo pairs that line up |
| F11 | **Friday ritual can grow too long.** "About 10 minutes"; data entry is the #1 quit reason (44%). | Krebs & Duncan 2015 | A 1-minute path: review → «شروع هفته» with plan steps optional | S | Rituals completed / weeks |
| F12 | **Make "no internet" literal.** Architecture lists no `INTERNET` permission; nothing enforces it. | Iran shutdowns; trust complaints elsewhere | Add a build check that fails if `android.permission.INTERNET` appears in the merged manifest (Coil without its network module) | S | Check passes |
| F13 | **Home-screen widget** is "after MVP". | One case study: widget users +25% retention (selection bias; Android Developers Blog 2026) | Keep after MVP, but before week 8; a Today widget is an always-visible environmental cue | M | Taps from widget vs notification (F8) |

## Priority 3: conditional or low impact now

| # | Finding | Note |
|---|---|---|
| F14 | **Accessibility.** Icon-only tabs and tiles need content descriptions; grid levels are color-only; Persian text must survive 130% font scale; tiles with long names at large font. | Low impact for one sighted user; **required** before any release. M3 gives most of it if content descriptions are set. Add a checklist item per slice. |
| F15 | **Positioning and name.** "BeAesthetic" is English and near "looksmaxxing" culture; the Persian name is open (Q10). | Only matters for a release. Pick a Persian name that says "daily coach" not "looks". `07` §1. |
| F16 | **Onboarding and monetization.** Onboarding is last (fine: seed data). A release would need starter programs and a price model. | `07` §3, §5. Recommend: no monetization; if ever, one-time on Bazaar. |

---

## Things the audit checked and found already right

| Area | Why it's right |
|---|---|
| No streaks, gray gaps, no red | Broken-streak research; GitHub natural experiment; Habitica/75 Hard failure modes |
| Minimum day | Duolingo's separate "show up" bar; Sharif & Shu slack |
| Special days capped at 2 | Emergency-reserve evidence |
| Edit window (today + yesterday) | Repair attenuates broken-streak effects |
| One notification per block, absence reduction | Notification fatigue; habituation |
| Pre-filled sets, one-tap logging | Data-entry burden is the top quit reason |
| Verified backups, restore tested | Data loss is a top complaint for Strong and Finch |
| Offline, no accounts, no Google | 2022 FCM disruption; 2026 blackouts |
| Wins-only Friday review | Positive reflection without listing misses |
| Progression *suggested*, never automatic | Static goals ≥ adaptive (Adams 2017); autonomy |

## Things not to change

The visual direction, the four tabs, the scoring thresholds (tune after 2 weeks as planned), the reminder architecture. None of the research argues against them.
