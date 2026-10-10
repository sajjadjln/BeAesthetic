# 05 · Gamification research

_Research date: 2026-10-10. Sources are **[Sourced]** from search excerpts of papers and company posts (full texts were not reachable; see `01` → Method). Numbers are as reported by those excerpts; check the original before quoting them elsewhere. Full list in `SOURCES.md`._

---

## 0. The frame: what gamification is for in this app

BeAesthetic's goal is **not engagement**. It's **doing the program**: routines done, workouts logged, body changed by 20 March 2027, and some habits still alive afterwards. More time in the app is neutral at best. A mechanic earns its place only if it helps the user **do the thing in real life**, especially on bad days and in the boring middle.

Three findings set the frame:

1. **Gamification works, modestly, and fades.** A meta-analysis of 16 RCTs (2,407 people) found gamified interventions raised physical activity with a small-to-medium effect (g = 0.42, about **+1,600 steps/day**), shrinking to g = 0.15 at follow-up (Mazeas et al., JMIR 2022). In the STEP UP trial (602 adults, 24 weeks), points-and-levels games with social elements raised steps; after the game ended, only the competition arm stayed above control (Patel et al., JAMA IM 2019). **[Sourced]**
2. **Most nudges stop working when they stop.** In the 61,293-person gym megastudy, 45% of programs raised visits during the program, but **only 8% still had a measurable effect after it ended** (Milkman et al., Nature 2021). Loss-framed money raised step goals met (45% of days vs 30% control), and **steps fell in every group once incentives ended** (Patel et al., Annals 2016). **[Sourced]**
3. **Long-term exercise is predicted by autonomous motivation.** A review of 66 studies found autonomous motivation consistently predicts exercise; intrinsic motivation better predicts **long-term** adherence, and feeling competent predicts participation (Teixeira et al., IJBNPA 2012). Expected, tangible, completion-contingent rewards **reduce** free-choice intrinsic motivation (Deci, Koestner & Ryan 1999, 128 studies; disputed by Cameron & Pierce). **[Sourced]**

**So [Inference]:** in a 23-week program, prefer mechanics that build **competence** (visible progress, PRs), **autonomy** (the user sets the plan, picks the floor, plans special days) and **meaning** (the finish line, identity), over mechanics that pay points for actions.

### Intrinsic vs. extrinsic, applied

| Need (SDT) | What feeds it | Already in BeAesthetic | Gap |
|---|---|---|---|
| **Competence** | Progress you can see, PRs, mastery | Strength tests, PRs, "since Day 0", charts | PRs appear only in the Friday review and Progress, not at the moment they happen |
| **Autonomy** | Choosing goals, rules, exceptions | User edits tasks, workout plan, minimum day, special days | None major |
| **Relatedness** | Feeling connected | None (social is out of scope) | Deliberate; see mechanic 9 |
| **Purpose / meaning** | A goal that matters | Finite program, finish line | The finish line isn't framed (Nowruz eve) |
| Extrinsic: points, currency, status | — | None | Good. Keep it that way. |

---

## 1. Mechanic-by-mechanic evaluation

Format per mechanic: **What** · **Motivation** · **Examples** · **Evidence** · **Risks** · **Complexity** (S/M/L) · **Relevance** · **Verdict**.

### 1. Points, XP and scoring
- **What:** numeric reward per action, often summed into XP.
- **Motivation:** extrinsic achievement.
- **Examples:** Duolingo XP; Habitica XP and gold; Todoist Karma.
- **Evidence:** points + levels were part of STEP UP's effective arms (with social elements, so not isolated). Completion-contingent rewards can lower intrinsic motivation (Deci et al. 1999). **[Sourced]**
- **Risks:** turns brushing teeth into a points chore; inflation; "farming" easy tasks.
- **Complexity:** S. **Relevance:** low. BeAesthetic already has a better score: % of the day done → level 0–4.
- **Verdict: Avoid** (XP). **Keep** the existing day level as the only score.

### 2. Levels, ranks and progression
- **What:** status tiers unlocked by accumulated points.
- **Examples:** Habitica levels; STEP UP levels.
- **Evidence:** as above, not isolated.
- **Risks:** levels for a 23-week program are arbitrary; a rank is meaningless without others.
- **Complexity:** M. **Relevance:** low as ranks; **high as program phases** (see 7, 16).
- **Verdict: Avoid ranks. Use program chapters instead** (mechanic 7).

### 3. Daily and weekly streaks
- **What:** count of consecutive days/weeks; resets on a miss.
- **Motivation:** loss aversion, consistency.
- **Examples:** Duolingo, Snapchat, Apple awards, GitHub (until 2016), «عادت‌ساز».
- **Evidence:** Streaks drive activity: Duolingo's streak-at-top test gave **+3% DAU**; GitHub's counter removal **cut weekend activity and long streaks** (Moldon et al., ICSE 2021). But **broken streaks demotivate**: showing a broken streak lowers later engagement compared with an intact one, **regardless of actual past behavior**, more so when people blame themselves (Silverman & Barasch, JCR 2023). Missing a single day **didn't materially affect habit formation** (Lally et al. 2010; small sample, not replicated). **[Sourced]**
- **Risks:** anxiety, "keep the streak" low-value actions (GitHub's one-commit days), quitting after a break, the exact "bad week makes me avoid looking" failure the user named.
- **Complexity:** S. **Relevance:** high risk, low need.
- **Verdict: Avoid** (already banned). The rolling "showed up X of the last 7 days" is the right substitute.

### 4. Streak recovery and flexible participation
- **What:** freezes, repairs, pauses, days off, "never miss twice".
- **Examples:** Duolingo Weekend Amulet (**+4% returning after a week, −5% streak loss**) and streak repair; Apple ring pause (up to 90 days); Todoist vacation mode and 3 restores; Forte and Loop scores.
- **Evidence:** Silverman & Barasch: the ability to **repair** a broken streak **attenuates** the demotivation. Sharif & Shu: goals with **emergency reserves** (a few allowed skips) produced more persistence after a failure (rebound **55%** with weekly reserves vs **37%** for hard goals, field step study, 270+ people). Milkman megastudy: a bonus for **returning after a missed workout** was the top program (**+27%** visits). **[Sourced]**
- **Risks:** too much slack becomes no goal (Duolingo's "too easy" test showed no gain).
- **Complexity:** S–M. **Relevance: very high.**
- **Verdict: Implement** (mostly done: special days = emergency reserves; minimum day = floor; edit window = repair). **Add** a positive "comeback" acknowledgment (see `06`, R2).

### 5. Badges, achievements and milestones
- **What:** one-off awards for reaching thresholds.
- **Examples:** Apple awards, Strava achievements, Duolingo badges.
- **Evidence:** no strong isolated evidence found. Milestones as **subgoals** help in the middle of long goals (see 7).
- **Risks:** badge clutter; meaningless "first tick!" badges; collection compulsion.
- **Complexity:** M. **Relevance:** medium **if tied to real-world proof** (first 10 pull-ups, −3 cm waist), low otherwise.
- **Verdict: Experiment** with a **few real milestones** shown in the Friday review and recaps. No badge wall.

### 6. Daily quests and weekly challenges
- **What:** short, changing objectives.
- **Examples:** Duolingo quests; Habitica quests; Strava monthly challenges.
- **Evidence:** novelty helps short-term; HeartSteps shows prompts **habituate** (effect decays about 2%/day, gone by day 28). **[Sourced]**
- **Risks:** the program already defines the work; extra quests compete with it; content upkeep.
- **Complexity:** M. **Relevance:** low–medium.
- **Verdict: Defer.** If boredom shows up, use **one optional weekly focus** chosen in the Friday plan (the user removed "weekly focus" in D24, so only if asked).

### 7. Long-term goals and progression paths
- **What:** a path broken into stages toward a big goal.
- **Examples:** Fabulous journeys; Duolingo course path; 75 Hard; C25K.
- **Evidence:** Motivation is **U-shaped**: high near the start and the end, **lowest around halfway**, because people switch reference points from "how far I've come" to "how far is left" (Bonezzi et al., Psych Sci 2011). The **goal-gradient** effect speeds people up near a reward (Kivetz et al., JMR 2006). **Fresh starts** (new week, month, semester, birthday) raise goal behavior (Dai, Milkman & Riis, Mgmt Sci 2014). **[Sourced]**
- **Inference:** splitting 23 weeks into **short chapters** creates many "near the end" moments and many fresh starts, and shrinks the boring middle.
- **Complexity:** S–M. **Relevance: very high.**
- **Verdict: Implement**: chapters aligned to **Jalali months** (Mehr/Aban, Azar, Dey, Bahman, Esfand), each ending with the monthly check-in. Pull the backlog's "5-week phases" forward and align it with months.

### 8. Leaderboards and friendly competition
- **What:** ranking against others.
- **Examples:** Duolingo leagues; Strava segments; STEP UP competition arm (+920 steps/day, only arm sustained after the game).
- **Evidence:** helps some, demotivates others; top ranks can make people complacent; effect depends on personality and rank (mostly education/crowdsourcing studies). **[Sourced]**
- **Risks:** shame for low ranks; needs other people and a server.
- **Complexity:** L. **Relevance:** none for one user.
- **Verdict: Avoid.** Self-competition (14) covers the useful part.

### 9. Teams, collaboration and social accountability
- **What:** partners, teams, shared goals.
- **Examples:** Habitica parties (with shared damage); STEP UP support arm (+689 steps/day); Finch friends.
- **Evidence:** monitoring works better when progress is **reported or made public** (Harkin et al., Psych Bull 2016; 138 studies, overall d = 0.40). **[Sourced]**
- **Risks:** server, accounts, privacy; shared punishment (Habitica) is toxic.
- **Complexity:** L (in-app), **S as a manual share** (export an image the user sends to a friend themselves).
- **Relevance:** medium; conflicts with SPEC §3.3 ("no sharing").
- **Verdict: Experiment, only if the user wants it**: a weekly "share card" image of wins, sent manually to one accountability partner. No accounts, no server.

### 10. Rewards, currencies, unlockables and collections
- **What:** earn currency, spend on items.
- **Examples:** Finch rainbow stones and cosmetics; Habitica gold and gear.
- **Evidence:** tangible expected rewards reduce intrinsic motivation (Deci et al. 1999). Immediate rewards beat delayed in an RCT (+2,762 vs +2,016 steps/day; Adams et al. 2017), but those were money/points. **[Sourced]**
- **Risks:** an economy to balance; rewards become the point.
- **Complexity:** L. **Relevance:** low.
- **Verdict: Avoid.** Don't gate the accent colors behind progress either: they're a free choice today, and locking them would cost autonomy for little gain.

### 11. Avatars, customization and identity
- **What:** a character or self-representation; identity framing ("I'm someone who trains").
- **Examples:** Finch pet; Habitica avatar; Atoms ("pick the kind of person you want to become").
- **Evidence:** identity-based framing is popular (Atoms) but evidence here is mostly practitioner writing. Customization supports autonomy.
- **Risks:** avatar upkeep; pet-as-chore (Finch reviews).
- **Complexity:** L (avatar), S (identity line).
- **Relevance:** customization already exists (accent color, theme). Identity: medium.
- **Verdict: Avoid avatars. Experiment** with one **identity sentence** written by the user at onboarding, shown in the Friday ritual and at the halfway recap.

### 12. Progress bars, completion indicators and mastery
- **What:** visual completion (rings, bars, grids).
- **Examples:** Apple rings; GitHub grid; Duolingo path.
- **Evidence:** **monitoring progress improves goal attainment** (d = 0.40), more when **physically recorded** (Harkin et al. 2016). **Endowed progress**: a card with 2 of 10 stamps done is completed more often than a blank card of 8 (Nunes & Drèze 2006). **[Sourced]**
- **Risks:** an always-unfinished bar demotivates; gray gaps.
- **Complexity:** S. **Relevance: very high.**
- **Verdict: Implement** (already: ring, dots, grid). **Add:** a whole-program progress line toward the finish (e.g. «هفته ۵ از ۲۳ · ۱۱۷ روز تا نوروز»). **Fix:** days before install should not show as gray gaps (endowed progress, no-shame) — see `08`.

### 13. Variable rewards and surprise elements
- **What:** unpredictable rewards (loot, random praise).
- **Examples:** slot-machine mechanics; Duolingo chests; Robinhood's scratch-off (criticized).
- **Evidence:** listed among attention-capture / addictive patterns (deceptive.design catalogue); no controlled evidence found for benefit in wellbeing apps. **[Sourced]**
- **Risks:** compulsion, distrust, against the product's calm tone.
- **Complexity:** M. **Relevance:** negative.
- **Verdict: Avoid** random rewards. **Small, honest variety is fine**: varied wording of real wins, so copy doesn't feel canned.

### 14. Personal bests and self-competition
- **What:** beating your own previous numbers.
- **Examples:** Strava PRs (66% of runners and cyclists hit a segment PR in H1 2025); Strong/Hevy PRs.
- **Evidence:** competence satisfaction predicts exercise (Teixeira et al. 2012). Novices gain strength fast: **~15% at 4 weeks**, +6% more by week 6 in one 6-week study (Wilson et al., EJAP 2023; small sample). Immediate rewards beat delayed (Adams et al. 2017). **[Sourced]**
- **Risks:** plateaus after the beginner phase; chasing PRs with bad form.
- **Complexity:** S. **Relevance: very high** for "proof early".
- **Verdict: Implement** (PRs exist). **Add** an **in-session PR moment**: when a logged set beats the previous best, show «رکورد» right there.

### 15. Personalization and adaptive difficulty
- **What:** goals adjust to performance.
- **Examples:** Fitbod, Hevy Trainer; adaptive step goals.
- **Evidence:** in a 4-month RCT, **static** goals did slightly better than adaptive ones (+2,630 vs +2,149 steps/day, p = .095) (Adams et al. 2017). **[Sourced]**
- **Risks:** goals moving under you; opacity.
- **Complexity:** M–L. **Relevance:** medium.
- **Verdict: Keep the current approach**: the plan stays fixed and the app **suggests** progression (+1 rep) only when every set hits the top of the range (SPEC §6.4). That's adaptive with the user in control. No further adaptation.

### 16. Narrative, missions and world-building
- **What:** story that frames the work.
- **Examples:** Zombies, Run!; Habitica quests.
- **Evidence:** 30-user interview study: narrative immersion → longer sessions and long-term use (Farič et al. 2021); small samples. **[Sourced]**
- **Risks:** content cost; childish for some.
- **Complexity:** L. **Relevance:** low as fiction; medium as **framing**.
- **Verdict: Defer fiction. Implement framing:** the program as a journey Day 0 → Yalda (halfway) → Nowruz (finish), with named chapters.

### 17. Feedback, celebration and satisfying interactions
- **What:** immediate, pleasant feedback on completion.
- **Examples:** Duolingo's lesson-complete screens; Finch; Things' checkbox animation.
- **Evidence:** immediate rewards beat delayed in a physical-activity RCT (Adams et al. 2017). Stawarz et al. (2015) found **positive reinforcement didn't help habit automaticity**. Read together: celebration helps **motivation in the moment**, not habit formation itself. **[Sourced + Inference]**
- **Risks:** over-celebration is noisy and gets ignored; time cost.
- **Complexity:** S. **Relevance:** high (the design is calm; risk of feeling cold).
- **Verdict: Implement, small:** haptic + short accent animation on tick; a slightly bigger moment when a part of the day completes and when the day reaches 100%. No confetti on every tap.

### 18. Loss aversion and commitment mechanisms
- **What:** something to lose (money, streak, HP) if you fail.
- **Examples:** stickK-style contracts; Habitica HP; 75 Hard restart; Patel 2016 loss-framed money.
- **Evidence:** loss framing **raised** goal achievement during the incentive (45% vs 35% gain-framed, 30% control) but effects **ended when incentives ended** (Patel et al. 2016). Punishment invites cheating (Habitica wiki). Self-attributed breaks hurt most (Silverman & Barasch). **[Sourced]**
- **Risks:** shame, cheating, quitting — exactly what the product rules forbid.
- **Complexity:** S–M. **Relevance:** conflicts with core rules.
- **Verdict: Avoid** loss mechanics. The **commitment** worth keeping is soft and positive: the Friday plan (committing to special days ahead) and «مهم» tasks.

### 19. Reminders and re-engagement loops
- **What:** notifications that prompt action or return.
- **Examples:** every app; Duolingo's bandit-optimized reminders (+0.5% DAU).
- **Evidence:** **reminders help repetition but hinder automaticity**; **event-based cues** build automaticity (Stawarz et al., CHI 2015). Reminders and streaks create **dependency**: when the app goes, the behavior often goes (Renfree et al. 2016). Suggestions **habituate** within weeks (HeartSteps). **Implementation intentions** ("when X, I'll do Y") have a medium-to-large effect on goal attainment (d = 0.65, 94 tests; Gollwitzer & Sheeran 2006). Self-reported: 37% would disable notifications at 2–5 per week from an app (Localytics 2017, vendor survey). **[Sourced]**
- **Risks:** muted app; habits that depend on the phone; nothing left after week 23.
- **Complexity:** design is done; additions S.
- **Relevance: critical.** It's the app's core value now (`TASKS.md`).
- **Verdict: Implement as designed** (block notifications, one follow-up, absence reduction, health check), **plus**: (a) an **anchor cue** per task ("بعد از صبحانه"), shown on the tile and in the notification; (b) a later option to **fade** reminders for routines that are clearly established.

### 20. Seasonal events and limited-time challenges
- **What:** time-boxed events.
- **Examples:** Strava Year in Sport (December); Duolingo's Streak Revival campaign (Q2 2026, 15.4M revived streaks, company-reported).
- **Evidence:** fresh-start effect around temporal landmarks (Dai et al. 2014). Resets help people coming off failure but can **demotivate** people doing well (Dai 2018, via secondary). **[Sourced]**
- **Risks:** FOMO; scarcity pressure.
- **Complexity:** S–M. **Relevance: high**, because the program spans **Yalda (21 Dec, week 11 — almost exactly halfway)** and ends on **29 Esfand, the eve of Nowruz**.
- **Verdict: Implement** as calendar moments, not limited-time pressure: a halfway recap at Yalda, a finish recap at Nowruz, each Jalali month as a chapter start.

---

## 2. Additional mechanics found relevant

| # | Mechanic | Evidence | Verdict |
|---|---|---|---|
| 21 | **Minimum / floor goal** ("minimum day") | Duolingo: separating "show up" from the full goal raised retention (+3.3% D14); but too-easy extensions didn't help. Sharif & Shu: slack improves persistence. **[Sourced]** | **Keep** (designed). Keep the floor real (≥ 3 meaningful items). |
| 22 | **Emergency reserves** (capped skips) | Sharif & Shu: rebound 55% vs 37%; the "emergency" label made people reluctant to spend them. **[Sourced]** | **Keep** special days (cap 2). **Experiment** with showing them as reserves («۲ روز ذخیره»). |
| 23 | **Implementation intentions / anchor cues** | d = 0.65 (Gollwitzer & Sheeran); event cues → automaticity (Stawarz). **[Sourced]** | **Implement**: one optional "after…" anchor per task. |
| 24 | **Strength score instead of streak** (internal) | Loop: 13-day half-life average. **[Fact: code]** | **Implement internally**: best routines + reminder fading. |
| 25 | **Recap / reflection** | Strava Year in Sport; Harkin: physically recorded monitoring works better. **[Sourced]** | **Implement**: halfway + finish recaps; the Friday review already exists. |

---

## 3. Published experiments: details and limits

| Experiment | Hypothesis / feature | Comparison | Outcome | Metric · period | Limitations |
|---|---|---|---|---|---|
| Duolingo Weekend Amulet | Protecting weekends keeps learners on streak | Learners not offered it (implied) | +4% return after a week; −5% streak loss | Week-later return; period not disclosed | Company blog; control details not disclosed |
| Duolingo goal ≠ streak | Any lesson should keep the streak, separate from the XP goal | Control (A/B) | +3.3% D14 retention; +1% DAU; +10.5% on a streak | 20 days | Same; engagement metrics only |
| Duolingo streak at top | Visibility raises return | Control (A/B) | +3% DAU; +1% D14 | Not disclosed | Same |
| Duolingo one-exercise extension | Easier extension raises DAU | Control (A/B) | No DAU gain | Not disclosed | Same |
| Duolingo notification bandit (KDD 2020) | Learn best reminder template per user | Strong baseline | +0.5% DAU; +2% new-user retention | Live test; 35-day dataset released | Peer-reviewed; engagement only |
| 24 Hour Fitness megastudy (Nature 2021) | 54 behavioral programs raise gym visits | Placebo control | Top (+27%): bonus for returning after a miss; 45% significant; 8% lasting | Weekly visits; 28 days + follow-up | Very large and preregistered; US gym members; small money incentives |
| STEP UP (JAMA IM 2019) | Game + social design raises steps | Control | Competition +920, support +689 steps/day; only competition persisted | Steps; 24 + 12 weeks | Overweight adults with wearables; social design not isolatable |
| Patel framing (Annals 2016) | Loss framing beats gain | Gain, lottery, control | Goal met 45% (loss) vs 35% (gain), 36% (lottery), 30% (control) | Days ≥ 7,000 steps; 13 weeks | Money incentives; effects ended |
| Adams 2x2 (BMC PH 2017) | Adaptive goals and immediate rewards | Static goals; delayed rewards | Static ≥ adaptive (n.s.); immediate > delayed (p = .009) | Steps/day; 4 months | 77% women; erratum exists |
| GitHub counter removal (ICSE 2021) | Natural experiment | Before vs after | Long streaks and weekend activity fell | Contributions; years | Observational |

**What generalizes to BeAesthetic [Inference]:** the **direction** of effects (flexibility helps, broken-streak displays hurt, return-after-miss matters, novelty fades, immediate feedback helps in the moment). **Not** the sizes: these are large populations of casual users; this app has one motivated user.

---

## 4. Sustainability checklist (applied to every recommendation in `06`)

| Check | Rule |
|---|---|
| Real-world value | Does it help do the program, or just open the app? |
| No punishment for breaks | Nothing decreases, resets or turns warm-to-cold because of a miss. |
| Notification budget | Today's budget: up to 4 block notifications + follow-ups only for «مهم» + 1 Friday ritual. No new notification types without removing one. |
| Fair on bad days | Minimum day and special days still give a positive outcome. |
| Autonomy | The user can turn it off. |
| Doesn't undermine purpose | No reward that makes the reward the point. |
| Survives week 24 | Builds a habit or a skill, not dependence on the app. |
