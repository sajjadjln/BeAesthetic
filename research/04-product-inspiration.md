# 04 · Product inspiration (outside the category)

_Research date: 2026-10-10. Evidence is **[Sourced]** from company posts, papers and press as surfaced by search; see `SOURCES.md`. I did not use these apps for this research._

The rule for this file: extract the **principle**, then ask whether it fits **one user, one 23-week program, no shame, no servers**. Famous ≠ fit.

---

## 1. Duolingo — streak engineering, done as science

| | |
|---|---|
| Behavior it encourages | Coming back every day for a few minutes. |
| Where in the journey | From day 1; strongest after a streak exists. |
| What Duolingo published | **[Sourced: Duolingo blog "Improving the streak", making.duolingo.com]** |
| | · **Weekend Amulet** (protects the streak over a weekend): learners offered it were **4% more likely to return a week later** and **5% less likely to lose their streak**. |
| | · **Separating the daily goal from the streak** (any lesson keeps the streak): **+3.3% day-14 retention, +1% daily active learners**, +10.5% of daily learners on a streak, within 20 days. |
| | · **Streak shown at the top of the app**: **+3% DAU, +1% day-14 retention**. |
| | · **Making the streak too easy** (one exercise extends it): **no DAU gain**; it mostly served the least engaged users. |
| | · 600+ streak experiments in four years (Lenny's Podcast summary). Notification timing bandit (KDD 2020): **+0.5% DAU, +2% new-user retention**. |
| Limits of the evidence | Company-reported; metrics are engagement, not learning; comparison groups and test lengths mostly not disclosed; huge sample of casual users. |
| Drawbacks | Streak anxiety; "streak over substance" behavior; engagement ≠ learning. |
| **Principle that transfers** | **Lower the bar for "showing up", but keep a real bar.** BeAesthetic's minimum day does exactly this. The "too easy" result says don't let a single tap count as a full day. **Keep the floor meaningful** (brush + creatine + 10 push-ups, not "open the app"). |
| **Principle that doesn't** | Streak visibility and loss framing. BeAesthetic has banned them for good reason (`05` §3). |

## 2. Strava — make effort feel seen

| | |
|---|---|
| Behavior | Recording every activity; returning for the social and personal record. |
| Mechanics | **Personal records** on segments (66% of runners and cyclists hit a segment PR in the first half of 2025, per Strava's mid-year report). **Year in Sport**: a personal, shareable recap each December (needs ≥3 activities). **Local Legends**: rewards the **most efforts** on a segment over 90 days, not the fastest, which rewards consistency. Weekly streak stickers (2026). **[Sourced]** |
| Evidence of effect | No published experiment found. Popularity of the recap is visible (staged rollout to all users) but not measured publicly. |
| Drawbacks | Social comparison; "Strava or it didn't happen"; over-training for segments. |
| **Principles that transfer** | (1) **PRs are the strongest early proof**: they arrive before visible body change. (2) **A recap at a meaningful moment** turns months of data into a story. (3) **Reward consistency, not just peaks** (Local Legends). |
| **Adaptation** | A **halfway recap** (around week 12, after Yalda) and a **finish-line recap** on 29 Esfand: total days shown up, PRs, strength deltas, photo pair Day 0 → now. Private, generated on the phone, optionally saved as an image. |

## 3. Apple Activity rings — the visual goal, now with rest built in

| | |
|---|---|
| Behavior | Closing three rings daily. |
| What changed | **watchOS 11 (Sept 2024): pause rings** for a day, a week, a month or longer (up to 90 days) **without breaking award streaks**, and **set a different goal for each weekday**. **[Sourced]** |
| Why it matters | The most successful ring-based fitness product decided that **rest days must not count as failure** and that **goals should vary by day**. |
| **Principle that transfers** | BeAesthetic's ring and **weekday-aware due lists** already do the second. The "special day" is a capped pause. Apple shows a large mainstream product moving the same way. |
| Note | Apple's rings are **automatic** (sensors). BeAesthetic's ticks are manual, so logging effort matters more. |

## 4. Todoist Karma — goals you set, with days off and vacation

| | |
|---|---|
| Mechanics | User-set daily/weekly goals; **chosen days off** don't count; **Vacation mode** keeps the streak; support can **restore a broken streak up to 3 times**. **[Sourced: Todoist help center]** |
| **Principle** | Even a streak-based product adds **planned exceptions**. BeAesthetic's Friday plan (special days set ahead) is the same idea, done better: planned in the weekly ritual. |

## 5. Loop Habit Tracker — a score that forgives

| | |
|---|---|
| Mechanic | Habit strength = exponential average with a ~13-day half-life for daily habits. **[Fact: source code]** |
| Behavior | Keep going after a miss, because one miss costs a little, not everything. |
| **Principle** | **Measure consistency as a slow-moving average, not a count since the last miss.** |
| **Adaptation** | Use it **internally**: to pick "best routines" for the Friday review, and to know when a routine is established enough to **fade its reminder** (`06`, `08`). Don't show per-task scores on Today. |

## 6. Finch — warmth and a reason to come back

| | |
|---|---|
| Mechanic | A pet that grows with your self-care; currency for cosmetics; gentle tone. **[Sourced]** |
| Behavior | Daily return, driven by care for the pet. |
| Drawbacks | The pet can become a chore; shallow tracking; the cosmetic economy needs a monetization engine. **[Sourced]** |
| **Principle** | **A warm voice at the moment of completion matters.** BeAesthetic's design is calm and minimal; it risks being cold. |
| **Adaptation** | No pet. Instead, small, specific, non-repeating Persian copy on wins ("۱۰ شنا بیشتر از روز صفر"), a soft haptic and animation on ticking, and a kind welcome-back. Cheap and in character. |

## 7. GitHub contribution graph — the grid BeAesthetic borrowed

| | |
|---|---|
| What happened | GitHub **removed the streak counter** from profiles in 2016, unannounced, but kept the contribution grid. A natural experiment (Moldon, Strohmaier & Wachs, ICSE 2021) found: **long streaks were abandoned, weekend activity dropped, days with a single contribution fell**, and some developers dropped their public "100 days in a row" goals. **[Sourced]** |
| Read both ways | The counter **drove** activity, including low-value "one commit to keep the streak" days and weekend work. Removing it reduced both. |
| **Principle** | **A grid without a counter is calmer but weaker.** BeAesthetic chose the calm version (no counter), which is right for a shame-free product. Expect the grid alone to be a **mild** motivator. The real drivers need to be elsewhere: Today, reminders, proof. |
| Risk to watch | Gray gaps are still visible. Silverman & Barasch (2023) show the **way the log shows a break** changes later behavior, independent of actual behavior (`05`). |

## 8. Zombies, Run! — narrative as motivation

| | |
|---|---|
| Mechanic | Story missions delivered as audio during runs. |
| Evidence | Interviews with 30 users: narrative immersion led to longer sessions and long-term use (Farič et al. 2021). Small treadmill study: lower perceived exertion. **[Sourced]** Small samples, self-report. |
| **Principle** | Story gives meaning to repetition. |
| **Fit** | Low as a feature (needs content). But the **program itself is a story**: Day 0 → Yalda → Nowruz. Name the chapters, don't write a game. |

## 9. 24 Hour Fitness megastudy — what actually moved gym visits

Not a product, but the best single source for this app. **[Sourced: Milkman et al., Nature 2021; Penn Today]**

- 54 four-week programs, **61,293 gym members**.
- 45% of programs raised weekly visits (by 9–27%) during the program.
- **The top program paid a tiny bonus for returning after a missed workout** ("don't miss twice"), **+27%**.
- **Only 8% of programs still had a measurable effect after the four weeks ended.**

**Principles:** (1) **The moment after a miss is the most valuable moment to design for.** (2) Most nudges fade when they stop, so a 23-week program needs **renewal**, not one clever trick.

---

## Summary: principles worth adopting

| # | Principle | From | Fit for BeAesthetic |
|---|---|---|---|
| 1 | Design for **the day after a miss** | Megastudy, Duolingo repair | **High.** Extend the welcome-back idea to a single missed day: count "comebacks" as wins. |
| 2 | **Lower the bar, but keep a real bar** | Duolingo | **High.** Already the minimum day. Keep its floor meaningful. |
| 3 | **PRs and recaps** make effort visible | Strava | **High.** Immediate PR moment + halfway and finish recaps. |
| 4 | **Planned rest isn't failure** | Apple, Todoist | **Already in design** (special days). |
| 5 | **Slow average, not a streak**, for consistency | Loop | **High, internal.** |
| 6 | **Warmth at completion** | Finch | **Medium.** Copy, haptics, small animation. |
| 7 | **Renewal over months** | Megastudy (8% persist), Fabulous journeys | **High.** Program chapters by Jalali month. |
| 8 | **A grid alone is a mild motivator** | GitHub | **Expectation-setting.** Don't rely on it. |
| 9 | Story | Zombies, Run! | **Low** as a feature; use naming only. |
