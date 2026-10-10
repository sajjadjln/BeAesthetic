# 02 · Competitor analysis

_Research date: 2026-10-10._

**Read this first: how much was actually inspected.** Page access was blocked in this environment (see `01` → Method). I could **not** open store listings, competitor websites or app screens. Except where marked **[Fact: code]**, every claim below is **[Sourced]** from search-result excerpts of reviews, store listings, help centers and research, with links. Official websites are listed so you can check them, but I didn't open them. I did not install or use any of these apps.

---

## How competitors were chosen

There is **no direct competitor** in the strict sense: no app found combines a finite body-transformation program, daily routines, workout logging and body proof, offline and shame-free. So:

- **Direct** = apps that compete for the **daily-routine** job (the Today screen), which is BeAesthetic's main job (SPEC principle 1). 6 profiled.
- **Indirect** = apps or methods that compete for the **training** and **proof** jobs, or replace the app entirely. 7 profiled.
- **Adjacent inspiration** = products from other categories with mechanics worth studying. Covered in `04`.

---

## A. Direct competitors (daily routines)

### A1. Loop Habit Tracker — the closest technical twin

| | |
|---|---|
| Links | https://github.com/iSoron/uhabits · https://loophabits.org (not opened) |
| What it is | Free, open-source (GPLv3) Android habit tracker. No ads, no account. **[Sourced]** |
| Audience | Android users who want simple, private habit tracking. **[Inference]** |
| Positioning | "Get your life on track"; a few missed days after a long run **don't wipe out progress**, unlike "don't break the chain" apps. **[Sourced: loophabits.org via search]** |
| Distinctive | A **habit strength score** instead of a streak. **[Fact: code]** In `Score.kt`, each day: `multiplier = 0.5^(sqrt(frequency)/13)`; `score = previous × multiplier + checkmark × (1 − multiplier)`. For a daily habit that's a **13-day half-life**: one miss dents the score slightly, a long gap fades it, and it recovers as you resume. (Repo commit of 2026-07-21.) |
| Features | Habit list, flexible schedules (e.g. 3 times in 8 days), reminders, widgets, charts, history, CSV/SQLite export. **[Sourced, partial]** |
| Business | Free; donations. No acquisition spend found. Distributed via Play Store and F-Droid. **[Sourced]** |
| Feedback | Not retrieved (Play page blocked). |
| **Lessons for BeAesthetic** | (1) A decaying strength score is a proven, open, shame-free alternative to streaks, and it's 3 lines of code. It fits `:domain`. (2) Being open, private and free earned it a loyal audience with no marketing. **[Inference]** |

### A2. HabitNow — the Android routine planner

| | |
|---|---|
| Links | https://www.alternativeto.net/software/habitnow/about/ (secondary); Play listing not opened |
| What it is | Android habit tracker plus to-do list: habits, recurring tasks, subtasks, reminders, widgets, calendar views, statistics. **[Sourced]** |
| Business | Free tier with a habit cap (sources disagree: 3, 5 or 7 habits); premium unlock reported as one-time ($5.99–$29.99) or subscription ($4.99/mo, $19.99/yr). **[Sourced, conflicting]** |
| Feedback | ~92k Play reviews (blog figure). Praised: easy interface, task management, affordability. Complaints: number-entry bugs, no Arabic, more payment options wanted. **[Sourced: Kimola review summaries]** |
| **Lessons** | Habits and one-off tasks together is popular on Android, which supports BeAesthetic's "repeat once" tasks (D25). A cap on the free tier is the common way to monetize this. |

### A3. Finch — the shame-free, cozy tone leader

| | |
|---|---|
| Links | https://finchcare.com (not opened) |
| What it is | A "self-care pet" app: completing goals gives your bird energy; you earn "rainbow stones" to buy clothes and furniture for it. **[Sourced]** |
| Audience | People who find classic habit trackers cold or judgmental; strong with anxious or depressed users per reviews. **[Sourced + Inference]** |
| Positioning | Playful, non-judgmental self-care. **[Sourced: reviewer characterizations; no formal "no punishment" policy found]** |
| Onboarding | A personalization quiz, then a **soft paywall** and a 7-day free trial of Finch Plus. **[Sourced]** |
| Growth | Invite system that rewards users with in-app currency and items for bringing friends. **[Sourced]** |
| Feedback (praise) | Motivational tone; positive overall sentiment in App Store review analyses. **[Sourced]** |
| Feedback (complaints, recurring) | **Billing:** "start your 7 days free trial" pop-up led to an immediate €79.99 annual charge (Trustpilot, May 2025); similar report Oct 2025. **Data:** "my pet data is corrupted" after a forced logout (Mar 2026). **Depth:** shallow tracking; the pet can become a chore; no dedicated streak/stats page. **[Sourced]** |
| **Lessons** | Warmth and a non-judgmental voice drive love. A companion creature is a strong motivator for some people, but it's a whole product (art, economy) and not BeAesthetic's identity. The billing and data-loss complaints show that **trust failures** kill goodwill fast. |

### A4. Fabulous — the guided routine coach

| | |
|---|---|
| Links | https://www.thefabulous.co (not opened) |
| What it is | Routine planner with guided "journeys", claims Duke-linked behavioral science. **[Sourced]** |
| Business | Short free trial converting to a paid subscription; little lasting free value. **[Sourced]** |
| Feedback | ~4.0 from ~588k reviews (May 2026, aggregator); Trustpilot ~3.4. Praise: habit guidance and structure. Complaints (recurring): charges after cancellation, trouble cancelling, "bloated interface", frequent paywalls. **[Sourced]** |
| **Lessons** | Structured "journeys" (a guided path over weeks) are valued. Aggressive monetization damages trust. BeAesthetic's 23-week program is a journey with a finish line, which is the part worth keeping. |

### A5. Habitica — the RPG that punishes

| | |
|---|---|
| Links | https://habitica.com (not opened) · https://habitica.fandom.com/wiki/FAQ |
| What it is | Habits, dailies and to-dos as an RPG: XP, gold, gear, pets, party quests. **[Sourced]** |
| Distinctive | **Missed Dailies cost health (HP).** At zero HP you lose a level, all gold and a piece of gear. In party quests, your misses damage the whole party. **[Sourced]** |
| Side effects | The official wiki notes players may be tempted to **check off tasks they didn't do** to avoid the loss. Guides advise keeping only 3–5 Dailies to avoid penalties. **[Sourced]** |
| **Lessons** | The clearest example of loss-based design and its failure mode: punishment invites cheating and shrinking your goals. It's the opposite of BeAesthetic's rules, and a good argument for keeping them. |

### A6. Persian habit apps on Cafe Bazaar: «عادت‌ساز» and «کندوی عادت‌ها»

| | «عادت‌ساز» (`com.simplgenia.tark`) | «کندوی عادت‌ها» Habit Hive |
|---|---|---|
| Link | https://cafebazaar.ir/app/com.simplgenia.tark | https://cafebazaar.ir/app/ir.amata.software.habit_hive |
| What it does | Daily habits, **Jalali dates**, consecutive-day counter, charts. Private, **offline, no ads**. **[Sourced]** | Based on *Atomic Habits*. **[Sourced]** |
| Price | 16,000 toman, one-time. **[Sourced]** | Not found |
| Notable | **"If you skip even one day, the counter starts over."** **[Sourced, Persian summary of the listing]** | 4.6 from only 9 ratings. **[Sourced]** |
| **Lessons** | The local supply covers Jalali and offline, but it is basic and uses resetting streaks. No Persian app found combines habits with training and body proof. **[Inference; Bazaar wasn't searched exhaustively]** |

---

## B. Indirect competitors (training, proof, or replacing the app)

### B1. Strong — the classic workout log

| | |
|---|---|
| Links | https://www.strong.app (not opened) |
| What it is | Fast set/rep/weight logger with rest timer, history and charts. **[Sourced]** |
| Price | PRO $4.99/mo, $29.99/yr, or $99.99 lifetime. **[Sourced]** |
| Feedback | ~4.8 on the App Store. **Praise:** speed and simplicity of logging. **Complaints (recurring):** workouts deleted or "lost all my workouts after an update", crashes, Apple Watch sync, weak support, "fallen behind". **[Sourced]** |
| **Lessons** | Speed is the product. Showing last session's numbers at entry is what lifters value most (general reviewer point; exact Strong UI not verified). Data loss is the complaint that turns a lifetime buyer into a critic, which supports BeAesthetic's backup-first stance. |

### B2. Hevy — the social workout log

| | |
|---|---|
| Links | https://www.hevyapp.com (not opened) |
| What it is | Workout logger with a social feed, routines, measurements, and "Hevy Trainer" (AI routine generator). **[Sourced]** |
| Price | Free with caps (4 routines, 7 custom exercises, 3 months of stats, per reviews); Pro $2.99/mo, $23.99/yr, $74.99 lifetime. **[Sourced]** |
| **Lessons** | A generous free logger plus a low-cost Pro is today's norm. BeAesthetic doesn't need any of it, but the pre-fill-from-last-session pattern is now standard and expected. |

### B3. FitNotes — the offline Android log

| | |
|---|---|
| Links | http://www.fitnotesapp.com (not opened) |
| What it is | Free, no ads, offline, no account, local storage, CSV export, rest timer. Small exercise library, no videos. **[Sourced]** |
| Feedback | "The gym log I wish I'd found years ago" (Android Authority-network review). **[Sourced]** |
| **Lessons** | Proof that a focused, private, offline logger can be loved. Same values as BeAesthetic. |

### B4. Guided bodyweight programs: Thenx, Madbarz, Caliverse, Leap Fitness "Home Workout"

| | |
|---|---|
| What they do | Video-led calisthenics or no-equipment programs, multi-week courses. **[Sourced]** |
| Price | Thenx $9.99/mo ($89.99–$119.99/yr); Madbarz $9.99/mo or $59.99/yr; Leap Fitness apps free with ads/IAP. **[Sourced]** |
| In Iran | Leap Fitness apps are on Bazaar in Persian translation; a Persian no-equipment app has reviews complaining about ads and silent videos. **[Sourced]** |
| **Lessons** | They sell **content** (programs, videos). BeAesthetic sells **adherence** to a program the user already has. Different job; don't compete on content. |

### B5. Progress-photo apps: Metamorph, PhotoJourney, Progressly, EZ Progress

| | |
|---|---|
| What they do | Photo timelines with **ghost overlay** of the last photo for identical framing, side-by-side and swipe compare, time-lapse export; some on-device only. **[Sourced]** |
| **Lessons** | Ghost overlay and a compare slider are the expected features for photo proof. Both are already in BeAesthetic's backlog. The research supports pulling the ghost camera forward to before the 2nd photo set (≈ mid-November). |

### B6. 75 Hard — the all-or-nothing challenge

| | |
|---|---|
| What it is | A 75-day challenge with strict daily rules. **Miss any task and you restart at day 1.** **[Sourced]** |
| Reception | Very popular; experts quoted by CNN say the rigid approach "may undermine the long-term behavioral changes it promises"; no supporting studies; a "75 Soft" variant drops the restart rule. **[Sourced]** |
| **Lessons** | A finite, dated program with a clear finish line is compelling, and that's BeAesthetic's shape too. The restart rule is the part to avoid. |

### B7. Non-app substitutes: an online coach, paper, spreadsheets

Covered in `01` §4. The Iranian online-coach model (plan by DM, no tracking) is the most relevant substitute for a future Bazaar audience. **[Sourced + Inference]**

---

## C. Customer feedback: patterns across competitors

| Pattern | Evidence | Strength |
|---|---|---|
| **Trust breaks loyalty fastest** (surprise billing, data loss) | Finch, Fabulous billing; Strong, Finch data loss | Recurring across 4 products |
| **Speed of logging is praised more than features** | Strong, FitNotes reviews; 44% quit over data entry | Recurring |
| **Punishment produces cheating or shrinking goals** | Habitica wiki; 75 Hard criticism | Documented design effect, plus research (`05`) |
| **Warm, non-judgmental tone creates love** | Finch reviews | Recurring for Finch |
| **Novelty fades** (pet becomes a chore; loss of interest) | Finch reviews; 40% "lost interest" | Recurring |
| **Beginners vs experienced** | Beginners value structure and guidance (Fabulous, Finch); experienced lifters value speed and data (Strong, FitNotes) | **[Inference]** from who praises what; no direct segment data |

**Features that appear to create loyalty [Inference from praise patterns]:** fast logging, visible history, data you own (export), a forgiving progress measure, and a warm voice.

**Missing features users request (where found):** more payment options and Arabic support (HabitNow); better stats (Finch). Most request data couldn't be retrieved because store pages were blocked.

---

## D. Research limitations for this file

- No screens, onboarding flows or notifications were observed first-hand.
- Ratings, prices and review counts come from secondary sources and conflict in places (noted inline).
- Review quotes are short excerpts as surfaced by search; I couldn't verify full context or check whether the reviewer later updated it.
- Bazaar coverage is a sample from searches, not a full category scan.
