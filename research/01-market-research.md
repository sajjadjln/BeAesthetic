# 01 · Market research

_Research date: 2026-10-10. Method and limits: see the end of this file. Labels: **[Fact]** observed · **[Sourced]** a cited claim · **[Inference]** my reasoning · **[Hypothesis]** to test._

---

## Summary: what matters for BeAesthetic

1. **The category is crowded and leaky.** Health & fitness apps keep only about **3–4% of users by day 30**, the lowest of most app categories. BeAesthetic's real competitor is not another app. It's **the user quitting**, which is the norm in this category. **[Sourced, §7]**
2. **The top stated reasons people stop using health apps are logging effort (44%), losing interest (40%) and hidden costs (36%).** BeAesthetic's one-tap, pre-filled, no-cost design hits all three. Losing interest is the one it can't design away, and it lines up with the user's own weak point ("quits when bored"). **[Sourced, §8]**
3. **Nobody we found combines routines + training + body proof in one shame-free app.** The market splits into habit trackers, workout loggers and progress-photo apps. Users stitch them together or use paper. **[Inference from §4–5; searches were not exhaustive]**
4. **In Iran, "works offline with no foreign servers" is not a nice-to-have.** Iran's internet was cut on 8 Jan 2026, and by May 2026 reports counted about 120 days without normal international access. FCM push was already disrupted in 2022. An app that depends on foreign servers can stop working overnight there. **[Sourced, §10]**
5. **The Persian-language part of the market is thin.** Cafe Bazaar workout apps found in searches are either translated global apps or Persian apps last updated in 2014–2015. Persian habit apps exist but are simple, and at least one resets the counter after a single miss. **[Sourced, §10; not exhaustive]**

---

## 1. Market category and subcategories

BeAesthetic sits where four subcategories overlap **[Inference]**:

| Subcategory | Job | Examples |
|---|---|---|
| Habit and routine trackers | "Help me do the small daily things" | Loop, HabitNow, Streaks, Habitica, Finch, Fabulous, Atoms |
| Workout loggers | "Record sets and show progression" | Strong, Hevy, FitNotes, JEFIT |
| Guided training programs | "Tell me what workout to do" | Thenx, Madbarz, Caliverse, Freeletics, Leap Fitness "Home Workout" |
| Body-progress trackers | "Show me my body is changing" | Metamorph, PhotoJourney, Happy Scale |

Adjacent: self-improvement / "glow-up" apps (including the controversial AI "looksmaxxing" apps), reminder/alarm apps, and planners.

**Where BeAesthetic is unusual [Inference]:** it is built around **one finite program with a finish line**, not open-ended habits or open-ended training. Among products reviewed, only challenge formats like 75 Hard share that shape, and they use the opposite philosophy (miss anything, restart at day 1).

## 2. Customer segments and their needs

| Segment | Core need | Evidence |
|---|---|---|
| Beginners starting a "transformation" | A clear plan; visible early change; not feeling judged | 75 Hard's popularity and the criticism of its harshness (§5); Finch's appeal as "non-judgmental" **[Sourced]** |
| Lifters who already train | Fast logging, last session's numbers, progression | Strong and Hevy are praised for speed and simplicity **[Sourced]** |
| Habit-app users who "fell off" | Flexibility, no punishment for missing days | Forte, Loop and "streak forgiveness" apps market exactly this **[Sourced]** |
| Privacy-minded / de-Googled users | Offline, no account, local data, export | FitNotes and Loop are praised for offline use and no account **[Sourced]** |
| Iranian users | Persian, Jalali dates, works without Google services, works during shutdowns | §10 **[Sourced + Inference]** |

## 3. Problems customers are trying to solve

From reviews and research, the recurring jobs **[Sourced + Inference]**:

1. **Remember** the small daily things (skincare, teeth, supplements).
2. **Know what to do** today without thinking.
3. **See progress** before the mirror shows it.
4. **Recover after a bad day or week** without feeling like a failure.
5. **Keep records safe** (data loss is a top complaint about Strong and Finch, §2 of `02`).

## 4. Existing alternatives (including non-app)

| Alternative | Why people use it | Why it fails |
|---|---|---|
| Paper habit tracker / bullet journal | Free, satisfying to fill, visible on a desk | No reminders, no workout history, no charts **[Inference]** |
| Notes app + phone alarms | Already on the phone | No structure, no proof of progress **[Inference]** |
| Spreadsheet | Total control (fits a developer) | High logging effort, the #1 quit reason **[Sourced, §8]** |
| A coach on Instagram or Telegram (common in Iran) | A personal plan, human accountability | Costs money; quality varies; the plan arrives as a PDF or message with no tracking **[Sourced: Iranian coaches advertise "online coaching" via DM; Inference on the rest]** |
| Several separate apps | Each is good at one job | Fragmented; more to open; more to forget **[Inference]** |

## 5. Market trends and emerging opportunities

| Trend | Evidence | Relevance |
|---|---|---|
| **Forgiving streaks** are becoming standard | Duolingo's Weekend Amulet and streak repair; Apple watchOS 11 lets users pause Activity rings for up to 90 days and set per-weekday goals; Todoist Vacation mode **[Sourced]** | BeAesthetic is already ahead: no streak at all. Validates the direction. |
| **Backlash against all-or-nothing challenges** | Experts criticize 75 Hard's "restart from day 1" rule; a softer "75 Soft" variant exists **[Sourced]** | Opening for a "hard program, kind rules" position. |
| **AI everything** (AI plans, AI body analysis) | Hevy Trainer (AI generator), GainFrame (AI body composition via Gemini), Umax (AI face rating) **[Sourced]** | Deliberately out of scope (SPEC §3.3). Also needs foreign servers, which fails in Iran. |
| **"Looksmaxxing" / glow-up culture among young men** | Umax: ~90% men 16–45, reported $500k/month (2024); 84% of 806k social mentions negative (2025–26) **[Sourced, secondary]** | The audience is real but the space has a toxic reputation. The name "BeAesthetic" sits near it. Position away from appearance-rating. |
| **Privacy-first, local-only apps** | Metamorph, FitNotes, Loop market "on-device" and "no account" **[Sourced]** | Matches BeAesthetic's architecture. |
| **Iranian users pushed to domestic or offline tools** by shutdowns | 2026 blackouts; domestic apps (Bale, Rubika) had outages **[Sourced]** | Offline-first is a real differentiator there. |

## 6. Customer pain points, complaints and unmet needs

Recurring across sources (details and links in `02`):

| Pain point | Where it shows up | Pattern or isolated? |
|---|---|---|
| Billing traps (trial converts to annual charge, hard to cancel) | Finch, Fabulous (Trustpilot 2025–26) | **Recurring** |
| Data loss after updates or account errors | Strong ("lost all my workouts after an update"), Finch ("pet data corrupted") | **Recurring** |
| Punishment for missing days | Habitica (HP loss, death), 75 Hard (restart), «عادت‌ساز» (counter resets) | **Recurring design choice**; harm documented in research (`05`) |
| Logging takes too long | 44% of people who quit health apps cite it | **Recurring** (survey) |
| Too many paywalls | Fabulous, Hevy free-tier caps | Recurring |
| Notifications that don't arrive on Xiaomi | Vendor help pages (Sofascore, Pushwoosh) describe MIUI/HyperOS killing apps | **Recurring** platform problem |

**Unmet need [Inference]:** one app that handles routines, training and body proof, forgives bad days, works offline, and doesn't take your money by surprise.

## 7. Retention in the category

| Source | Health & fitness day-30 retention |
|---|---|
| AppsFlyer (via Business of Apps, 2023 data) | 3% |
| Braze (via Business of Apps, 2026) | 3.9% iOS · 3.4% Android |
| UXCam | 3.7% |
| Adjust (via Business of Apps) | 8% (outlier) |

**[Sourced]** These are averages over millions of mostly casual installs. They are **not a baseline for BeAesthetic**, which has one highly motivated user who built it. They do show the shape of the problem: people drop off fast, and most drop-off happens in the first weeks.

## 8. Why people abandon health apps

| Reason | Share | Source |
|---|---|---|
| Entering data takes too much time | 44% | Krebs & Duncan 2015 (NYU, ~1,600 US phone owners) **[Sourced]** |
| Lost interest | 40% | same |
| Hidden costs | 36% | same |
| Confusing | >1/3 | same |
| Didn't like data shared with friends | 30% | same |

Other abandonment evidence:
- **Reminders create dependency.** Habit apps' reminders and streaks help repetition, but the behavior often stops when the app stops (Renfree et al., CHI 2016; Stawarz et al., CHI 2015). **[Sourced]** This matters for week 24 onward.
- **Former Fitbit users** stopped because tracking felt annoying or useless, or because they'd learned enough (Epstein et al., UbiComp 2016). **[Sourced]**
- **Motivation dips in the middle of a goal** (Bonezzi et al., 2011). For a 23-week program, the middle is around weeks 10–13 (mid-December to mid-January). **[Sourced + Inference]**

**Caveat:** the main survey is from 2015 and US-only. I found no 2024–2026 equivalent. The pattern (effort, boredom, cost) is consistent across sources.

## 9. Pricing expectations and monetization models

| Model | Examples (2026 prices from reviews, unverified on store pages) |
|---|---|
| One-time purchase | Streaks ~$5.99; HabitNow premium (one-time, $5.99–$29.99 reported); «عادت‌ساز» 16,000 toman on Bazaar; Strong lifetime $99.99; Hevy lifetime $74.99 |
| Subscription | Hevy Pro $2.99/mo or $23.99/yr; Strong $4.99/mo or $29.99/yr; Atoms $4.99/mo or $39.99/yr; Thenx $9.99/mo; Madbarz $9.99/mo or $59.99/yr |
| Freemium with caps | Hevy (4 routines, 3 months of stats free); HabitNow (habit cap) |
| Free and open source | Loop (GPLv3), FitNotes (free, optional supporter app) |
| Freemium + paid plans (Iran) | Karafs calorie counter: free tracking, paid diet plans |

**[Sourced]** Cafe Bazaar cut its commission from 30% to 15% on each developer's first 10 billion rials of yearly sales (2021). No newer figure was found. **[Sourced]**

**Implication [Inference]:** an offline, account-free app fits a **one-time purchase or free** model. A subscription would need a server to check entitlement over time, which goes against "no accounts" and is fragile during shutdowns. This only matters if a Bazaar release happens.

## 10. Geographic differences: Iran

| Factor | Evidence | Effect on the product |
|---|---|---|
| **App stores** | Cafe Bazaar: ~40–50M monthly users (secondary estimate, 2025); sold to Tapsell in Jan 2025. Google Play was unblocked in 2025. Myket ~8M users (2017 figure). **[Sourced]** | Bazaar is the channel if publishing ever happens. |
| **Google services** | FCM was disrupted for Iranian users in Aug 2022 (Digiato). Local push services exist (Pushe). **[Sourced]** | Validates local alarms instead of push. |
| **Internet shutdowns** | Blackout from 8 Jan 2026 (connectivity ~1–4% of normal per NetBlocks), relaxed 28 Jan; another after 28 Feb 2026; ~120 days without normal access by 8 May 2026 (The National). Domestic apps also had outages. **[Sourced]** | Any feature needing a server (accounts, sync, AI, subscriptions) can fail for weeks. **Offline-first is a survival trait in this market.** |
| **Language and calendar** | Persian-first, Jalali, Saturday week start, Friday weekend | Global apps rarely support Jalali or a Saturday start. **[Inference; not systematically checked]** |
| **Persian apps on Bazaar** | Workout: translated global loggers (GymKeeper, RepCount, My Workout Plan), Leap Fitness "Home Workout"; Persian-named workout apps found were last updated 2014–2015; a Persian no-equipment app has reviews complaining about ads. Habit: «عادت‌ساز» (one-time 16k toman, Jalali, offline, **counter resets after one miss**), «کندوی عادت‌ها» (Atomic Habits-based, 4.6 from 9 ratings). **[Sourced]** | Thin, dated Persian supply. A polished Persian-first, shame-free app would stand out. **[Inference]** |
| **Coaching culture** | Many Iranian fitness creators sell "online coaching" plans via Instagram DMs; audiences mostly 5k–25k followers; ~45% of one creator's audience in Tehran **[Sourced, influencer directories]** | Possible distribution partners; also a substitute (the coach's PDF). |

## 11. Niche markets and underserved audiences

| Niche | Why underserved | Confidence |
|---|---|---|
| Persian speakers doing home/bodyweight programs | Supply is translated or dated (§10) | Medium (searches only) |
| People on Xiaomi/HyperOS who need reliable reminders | Platform kills background apps; few apps guide setup | Medium |
| People who quit after breaking streaks | Most trackers still lead with streaks | Medium (research supports harm, `05`) |
| People with a dated goal (Nowruz, a wedding) | Few apps model a finite program with a finish line | Low–medium (no direct demand data) |

## 12. What I couldn't establish

- **Market size** for any of these niches in Iran or globally. I found no reliable figure and I'm not estimating one.
- **Market share** of Bazaar after Google Play was unblocked.
- **Current Bazaar reviews** of Persian fitness and habit apps (store pages couldn't be opened, see below).
- **Current, Iran-specific abandonment data.**

None of these change a near-term decision, because BeAesthetic has one user and no launch plan. They would matter before a Bazaar release.

---

## Method and limitations

- **Tools:** web search worked. **Opening pages directly was blocked** by this environment's network policy (Google Play, Cafe Bazaar, PubMed, dontkillmyapp.com, Duolingo's blog and most other sites returned "host not found" or a proxy 403). Only GitHub was reachable, so I read Loop Habit Tracker's scoring code directly.
- **So:** every claim from a web page comes from **search-result excerpts and secondary summaries**, not from my own reading of the page. Store ratings, prices and review counts are as reported by those sources and **may be out of date**. App screens were **not inspected**.
- **Fix:** allow the hosts `play.google.com`, `cafebazaar.ir`, `pubmed.ncbi.nlm.nih.gov` and `dontkillmyapp.com` under Network access in the cloud environment settings, then re-run the competitor checks. Steps: https://code.claude.com/docs/en/cloud-environments#network-access
- Full source list: `SOURCES.md`.
