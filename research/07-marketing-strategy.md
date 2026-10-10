# 07 · Marketing strategy (conditional)

_Research date: 2026-10-10._

## Read this first

BeAesthetic has **no marketing today and shouldn't**: it has one user, no listing, no website, and `CLAUDE.md` says not to add scope for a release. There is nothing to audit.

So this file answers one question: **if the user decides to publish on Cafe Bazaar after the program (spring 2027 or later), what should the approach be?** It's short on purpose. One thing in it matters **now**: a few cheap choices during the build keep this option open (§7).

---

## 1. Positioning

**Category the user would enter:** Persian body-transformation / self-improvement app (routines + training + progress). On Bazaar that's the «سلامتی و تناسب اندام» area, competing with translated global workout apps and simple Persian habit apps (`01` §10).

**Positioning statement (draft) [Inference]:**
> For Persian speakers starting a home body-transformation program who have quit apps before, BeAesthetic is a daily coach that tells you exactly what to do, reminds you reliably even on Xiaomi phones and during internet cuts, and never punishes a bad day. Unlike gym loggers or streak-based habit apps, it runs one finite program to a finish line and keeps everything on your phone.

**Is the value clear?** Today, internally, yes. Externally, "BeAesthetic" is English, close to "aesthetic/looksmaxxing" culture (which has a toxic reputation: 84% negative mentions in one 2025–26 analysis), and says nothing about routines or kindness. A Persian name (open question 10 in `TASKS.md`) is the bigger lever.

## 2. Differentiating messages (ranked by evidence)

| # | Message (Persian draft) | Proof behind it | Against whom |
|---|---|---|---|
| 1 | «بدون اینترنت کار می‌کند. داده‌هات فقط روی گوشی خودت.» | Offline-first; 2026 blackouts broke server-based apps (`01` §10) | Global and account-based apps |
| 2 | «یه روز بد، برنامه‌ات رو خراب نمی‌کنه.» (no streaks, minimum day) | Broken-streak research; 75 Hard criticism; «عادت‌ساز» resets | Streak apps, challenges |
| 3 | «یادآورها حتی روی شیائومی هم می‌رسن.» | Exact alarms + health check + setup guide | Every app with unreliable notifications |
| 4 | «تقویم شمسی، هفته از شنبه، جمعه تعطیل.» | Built in | Translated global apps |
| 5 | «۲۳ هفته تا نوروز.» (a finish line) | Goal-gradient and fresh-start research | Open-ended trackers |
| 6 | «یه بار بخر، برای همیشه.» (if paid) | Billing complaints dominate Finch/Fabulous reviews | Subscription apps |

**Benefits over features:** lead with 2 and 1 (feelings and safety), prove with 3–5.

## 3. Likely objections

| Objection | Answer |
|---|---|
| "Another habit app; I'll quit in a week." | Minimum day + no streak + welcome back; show the gray-gap grid in screenshots. |
| "I don't have a program." | **Real blocker.** Today the app assumes the user brings a plan. A public version needs starter programs or a simple program builder. Biggest scope item for a release. |
| "Will my data be lost?" | Automatic backups, export, restore. |
| "Why should I trust a small app with my photos?" | No internet permission needed for core use; say so in the listing. |

## 4. Channels (pick 3, not 10)

| Channel | Fit | Why | Priority |
|---|---|---|---|
| **Bazaar ASO** (Persian title with search words, screenshots, description) | High | The store is where Iranian Android users look; the listing is the landing page | **1** |
| **Iranian fitness creators on Instagram/Telegram** (small, 5k–25k followers) | Medium–high | Fragmented market, many coaches already sell plans by DM; a free tool for their clients is a natural partnership | **2** |
| **Persian content** (Virgool, Telegram channel, a short guide on "making reminders work on Xiaomi") | Medium | Solves a real, searchable pain; builds trust cheaply | **3** |
| Paid ads (Tapsell, Bazaar ads) | Low | Cost with no monetization to recover it | Skip |
| Referral program | Low | No accounts or server to track referrals | Skip |
| SEO website | Low | Store search matters more; a one-page site for the privacy note is enough | Minimal |

## 5. Conversion

- **Landing page = the Bazaar listing.** First screenshot should show Today with a minimum day, second the Dashboard grid with gray gaps and the line «روز بد مشکلی نیست».
- **Registration:** none. That's a conversion advantage worth stating.
- **Time to first value:** today the app needs program setup. For strangers, the first value must arrive in **under 2 minutes**: pick a starter routine set, get the first reminder tonight.
- **Pricing:** free core + one-time unlock, or fully free. Subscriptions conflict with "no accounts" and break during shutdowns. Bazaar's commission was 15% on the first 10bn rials/year (2021). The local reference price for a simple paid habit app is 16,000 toman one-time («عادت‌ساز»).

## 6. Retention and growth (if published)

| Lever | Status |
|---|---|
| Time to first value | Needs onboarding with starter programs |
| Activation event | **First reminder that arrives and gets ticked** (proposal) |
| Repeat triggers | Block reminders; widget (backlog) |
| Habit formation | Anchor cues (R4 in `06`) |
| Lifecycle communication | In-app only (no push servers): welcome back, chapter starts, recaps |
| Referral | None: the user ruled out sharing (D63). Word of mouth and the listing carry it |
| Churn prevention | Minimum day; comeback; reduced reminders after absence |

## 7. What matters now (cheap choices during the build)

1. **Keep personal data out of code** (already a rule; D45).
2. **Pick the Persian name with a release in mind**: say what it does, avoid "aesthetic" connotations. (Open question 10.)
3. **Design every screen so it can be screenshotted** for a listing: no personal names in seed data.
4. **Ship the recaps (R8)**: in the app only (D63), but a recap screen makes a strong listing screenshot.
5. **Keep the backup format documented**: it's a trust signal later.

## 8. Testable campaign ideas (for after a release decision)

| Idea | Hypothesis | Metric |
|---|---|---|
| "Before Nowruz" launch in Bahman–Esfand | Fresh-start demand peaks before Nowruz | Installs per day vs the month before |
| Xiaomi reminder guide as content | A practical guide brings high-intent users | Listing visits from the guide link |
| Creator pilot with 3 small coaches | Coaches will hand the app to clients | Installs per coach; clients active at week 4 |
| Listing A/B: "no internet needed" vs "no streaks" headline | One message converts better | Listing → install rate (if Bazaar supports listing tests; unverified) |

None of these have baselines. Don't forecast numbers until there's a listing.
