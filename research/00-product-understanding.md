# 00 · Product understanding

_Research date: 2026-10-10. Sources: the repository only (`CLAUDE.md`, `docs/*.md`, `docs/design/*.html`). No app code exists yet._

Evidence labels used in every research file:
**[Fact]** observed directly in the repo or a source · **[Sourced]** a claim made by a cited source · **[Inference]** my reasoning from facts · **[Hypothesis]** a guess to test.

---

## 1. Product overview

BeAesthetic is a **personal, offline-first Android app** that coaches one person through a **23-week body-transformation program** (9 Oct 2026 → 20 Mar 2027 / 17 Mehr → 29 Esfand 1405). **[Fact]**

It combines four things that are usually separate apps **[Fact, `SPEC.md` §3.1]**:

| Job | Feature in BeAesthetic | Usual separate app |
|---|---|---|
| "What do I do now?" | Today screen: day ring, 4 parts of the day, one-tap tiles, minimum day | Habit tracker / routine planner |
| "Don't let me forget" | Exact local alarms, one notification per block, one follow-up for «مهم» items, reminder health check, Xiaomi setup screen | Reminder app |
| "Log my training" | Workout session with pre-filled sets, 1–10 effort bar, PRs, progression suggestions, plan editor | Workout logger (Strong, Hevy) |
| "Prove it's working" | Dashboard grid (whole program), strength tests, weight, tape, monthly photos, "since Day 0" cards | Body/progress tracker |

Plus a **weekly Friday ritual** (wins-only review, special days, a snack note per day) and **automatic backups** to a Syncthing folder. **[Fact]**

**Stage:** Phases 1–4 (discovery, spec, design, architecture) are approved. Phase 5 (build) has not started. The program itself started on 2026-10-09, so the user is living the program **without the app** right now. **[Fact, `TASKS.md`]**

## 2. Value proposition

Draft, in the product's own terms **[Inference from `SPEC.md` §1 and principles]**:

> A coach that tells you exactly what to do today, reminds you even on a Xiaomi with no Google services, shows progress in weeks instead of months, and never punishes a bad day.

What makes it different from the apps it replaces **[Inference]**:

1. **One fixed, finite goal** (23 weeks, a finish line) rather than open-ended habits.
2. **Shame-free by rule**: no red, no resetting streaks, no list of misses, a "minimum day" floor, capped "special days".
3. **Body-transformation scope in one place**: routines (skincare, teeth, nails) + training + body metrics.
4. **Works in Iran**: no Google Play Services, Firebase, accounts or foreign servers; Persian-first RTL with Jalali dates; Saturday-start week; Friday weekend.
5. **Reliability as a feature**: reminder health log and a guided HyperOS/MIUI battery setup.

## 3. Known facts versus assumptions

### Facts (from the repo)

| Area | Fact |
|---|---|
| Users | Exactly one user today: the developer. A Cafe Bazaar release "may come much later"; no scope is added for it (D45). |
| Platform | Native Android (Kotlin, Compose, Room). `minSdk 26`. APK installed over USB. Poco X4 5G / HyperOS is the reliability target (D9, D31, D43, D44). |
| Business model | None. No pricing, no ads, no accounts, no analytics. |
| Marketing | None. No website, no store listing, no app name in Persian yet (open question 10). |
| Onboarding | Not designed. Planned last (slice 9) because the user is set up by seed data (D42). |
| Gamification present | Day ring; 7 week dots; day level 0–4 shown as accent intensity; GitHub-style 23×7 grid; weekly score chip; "showed up X of Y" (rolling, never resets); PRs («رکورد»); wins-only Friday review; "since Day 0" deltas; "minimum day" floor; "special day" (max 2/week); user-chosen accent color. |
| Gamification rejected | Streaks that reset, red, lists of misses, guilt wording, social features, sharing, leaderboards (SPEC §3.3). |
| Out of scope | Calorie/protein tracking, AI coach, Health Connect / wearables, full task manager, Play Store. |
| Later backlog | Goals with progress bars; before/after photo slider; ghost-overlay camera; meal planner; 5-week phases with new milestones; home-screen widget; sync to own .NET backend. |
| Known user weak points | Quits when bored, when change isn't visible fast, or with no clear goal; routines slip (skincare, brushing, nails, snack prep) (`CLAUDE.md`). |

### Assumptions (not verified)

| Assumption | Why it matters | How to check |
|---|---|---|
| A finite 23-week program with a finish line beats an open-ended tracker for this user. | It shapes every screen. | Track weekly use across weeks 1–23; watch the mid-program slump (weeks 6–14). |
| Strength numbers will move visibly in weeks 1–4. | It is the main "proof early" mechanism. | Strength test every 2 weeks; beginner strength gains in the first weeks are well documented (to be sourced in 05). |
| The contribution grid motivates without creating guilt. | Gray gaps are still visible; for some people any gap hurts. | Ask after week 2–3: "Do you avoid opening the Dashboard?" |
| Exact alarms via `setAlarmClock` survive HyperOS. | Missed reminders silently kill the habit loop. | Reminder health log after a week of real use (slice 2 exit test). |
| The Friday ritual (~10 min) will happen most weeks. | Weekly planning is the only "fresh start" moment. | Count completed rituals. |
| A future Bazaar audience would want the same thing. | Decides whether market/marketing research has any near-term value. | Not testable now; only matters if publishing becomes a goal. |

### What does not exist and must not be invented

Market size for BeAesthetic, user counts, revenue, retention baselines, conversion rates. **None exist.** Every metric in later files is a proposal for what to measure, not a baseline.

## 4. Target user hypotheses

**Primary user (fact, not hypothesis):** the developer. Male (inferred from beard trim / "aesthetic" program; not stated), ~mid-20s **[Inference from email, not confirmed; irrelevant to the design]**, Android developer, lives in Iran, Xiaomi phone, Persian/English, trains at home with bodyweight (pull-ups, push-ups, plank), hikes on Fridays.

**If the app were ever published (hypotheses only):**

| Segment | Why they might want it | Confidence |
|---|---|---|
| H1. Iranian young adults starting a home/bodyweight "glow-up" program | Persian-first, Jalali, works without Google services, offline | Low–medium. Needs evidence of demand (search trends, Bazaar category). |
| H2. People who quit habit apps after breaking a streak | Shame-free design, minimum day | Medium. Streak-shame complaints are common in reviews (to be sourced in 01/02). |
| H3. People on de-Googled or Chinese-ROM phones | Reliable alarms without FCM | Low. Niche. |
| H4. People with a dated goal (wedding, Nowruz, summer) | Finite program with a finish line | Medium. "Countdown to an event" is a known motivator (to be sourced). |

## 5. How this changes the research brief

The brief is written for a product with a market. BeAesthetic today has **one user and no business goal**. I'll still do every phase, but weighted this way **[my judgment]**:

| Phase | Weight | Why |
|---|---|---|
| 0, 3, 4, 6, 7 (product, inspiration, gamification, audit, experiments) | **High** | They change what gets built in slices 1–9 and whether the user is still using the app in week 23. |
| 1, 2 (market, competitors) | **Medium** | Useful mainly for features and UX patterns, and as a reality check for a Bazaar release. |
| 5 (marketing) | **Low, kept short** | Only relevant if a Bazaar release becomes a goal. I'll write a short, conditional strategy rather than a full plan. |
| Monetization | **Low** | Nothing to monetize. Covered as "what Bazaar users expect" only. |

"Retention" in this project means **the user still opens the app and does the program in weeks 6–23**. Experiments are mostly **n = 1 self-experiments**, so I'll design them as ABA (on/off/on) or week-by-week comparisons, not A/B tests.

## 6. Initial questions requiring further research

1. What do the best habit, workout and body-progress apps do that keeps people going past week 4, and what makes them quit? (01, 02)
2. What does the evidence say about streaks, streak freezes, "never miss twice", and flexible goals? (05)
3. Do minimum/"floor" goals and fresh-start moments have research behind them? (05)
4. How reliable are local exact alarms on HyperOS / MIUI, and what do other apps do? (01, 06)
5. What's in the Iranian market (Cafe Bazaar, Myket): Persian fitness/habit apps, how they monetize, what users complain about? (01, 02)
6. Which progress-proof mechanics (photos, measurements, strength tests) do users find most motivating? (02, 04)
7. What helps people through the "middle slump" of a long program? (04, 05)

## 7. Questions for the user (only the ones that change direction)

1. **Is a Cafe Bazaar release a real goal within ~12 months, or just a door left open?** I'm assuming "door left open" and keeping marketing short.
2. **Is it OK to recommend small changes to the approved design and the slice order?** I'm assuming yes, as proposals only. Nothing in `docs/` is changed by this research.
