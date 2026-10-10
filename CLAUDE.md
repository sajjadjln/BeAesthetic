# BeAesthetic

A personal Android app for one person (the user) that runs a 23-week body-transformation program from **9 Oct 2026 to 20 Mar 2027** (17 Mehr – 29 Esfand 1405). It tells the user what to do each day, logs workouts and body progress, and reminds reliably on a Xiaomi phone without Google services.

## Read these first, in order

| File | What it holds | Update it when |
|---|---|---|
| `docs/TASKS.md` | **Where we are now**: current phase, next steps, open questions, backlog | Every session that changes status |
| `docs/DECISIONS.md` | Every decision so far, with the reason, including reversals | A decision is made or changed |
| `docs/SPEC.md` | Product spec: vision, scope, rules (scoring, intervals, reminders, backup), data model | Product behavior changes |
| `docs/DESIGN.md` | Design system and the screen-by-screen design that is current | Visual or screen design changes |
| `docs/ARCHITECTURE.md` | Stack, modules, reminders, Room schema, backup, testing, build order | Technical design changes |
| `docs/design/screens.html` | **The current design**, an interactive HTML mockup of every screen | Screens change |

If these files disagree, the order of trust is: `DECISIONS.md` (newest entry wins) → `ARCHITECTURE.md` (technical) / `DESIGN.md` (visual) → `SPEC.md`. Then fix the file that is wrong.

## About the user

- A developer with an Android background and a .NET backend background. Reads and reviews code. Explain technical choices with the reasons behind them.
- Prefers **direct, opinionated recommendations** over a menu of equal options. When asking questions, ask 3–5 at a time, mostly multiple choice, mark the recommended option and give a one-line reason.
- Lives in **Iran**: Google Play Services, Firebase push and some APIs are unreliable. The app must not depend on them.
- Phone: **Poco X4 5G on HyperOS** (Xiaomi). It must work on any Android phone, but this phone is the reliability target.
- May publish the app on **Cafe Bazaar** much later. For now it's only for them. Don't add scope for it; just don't hard-code anything personal.
- Works in Persian and English. The app is **Persian-first (RTL)** with English as a second language.
- Their English is informal and sometimes has typos. Read for intent, and confirm when a message is truly ambiguous.
- Known weak points the app must work against: quits when bored, when change isn't visible fast, or when there's no clear goal; routines slip (skincare, brushing, nails, snack prep).

## How we work (the user's process)

1. **Discovery** → 2. **Spec** → 3. **Design** → 4. **Architecture** → 5. **Build** in small vertical slices.
- Don't skip phases, and don't assume answers. Ask when something is unclear.
- At the end of each phase, summarize the decisions made so far.
- In Phase 5: after each slice, show what works and what's next, then wait for feedback. Write tests for the logic that matters (reminder scheduling, date handling, progress calculations).
- Keep the first version small. A simple app used daily beats a big app that gets abandoned.

## Non-negotiable product rules

- **No shame mechanics.** Nothing is ever red. No streaks that reset. No lists of what was missed. No guilt wording. A missed day is a gray gap.
- **Today first.** The home screen answers "what do I do now?"
- **One tap to log.**
- **Local-first.** No Google Play Services, no Firebase, no analytics or crash SDKs, no accounts. Installed as an APK, not through the Play Store.
- **Never lose data** (automatic backups) and **never miss a reminder silently** (reminder health check).

## Conventions

- **Language:** Persian is the default, RTL; English is available. All UI strings live in resources and are never hard-coded. Digits follow the language (Persian digits in Persian).
- **Dates:** stored as Gregorian `LocalDate`. Jalali is presentation only, shown first with Gregorian second. The week starts **Saturday**. The day rolls over at **04:00**.
- **Time direction:** in RTL, time flows right → left (dashboard grid and charts).
- **Colors:** the accent = done/progress. The user picks it (green by default, or blue, turquoise, violet, orange). Amber = due or important (written as «موعدش رسیده» / «مهم»), yellow with the orange accent; indigo = special day (sick, travel, off), shifted for blue/violet; gray = gap. Palettes in `DESIGN.md`.
- **Font:** Estedad only, bundled in the APK.
- **Docs:** written in plain English, short sentences. UI copy is in Persian.

## Repository and environment notes

- **`main` is the main branch** (created 2026-10-10 from `claude/trusting-lovelace-e5b7iq`, which has the same history). Start new work from `main`.
- No app code exists yet. The repo holds only docs and HTML design mockups.
- **The app is built on the user's laptop in Android Studio** (it has the Android SDK) and installed over USB. Cloud sessions can't download the Android SDK (`dl.google.com` is blocked), so don't create or build the Android project there. Pure Kotlin (`:domain`) tests can run in a cloud session.
- **Figma** file https://www.figma.com/design/1DzXN6iOhAa0PsnLDSjOAg holds the first design-system pass. It is **out of date**. The user's Figma account is on the Starter plan: 3 pages, 1 variable mode per collection, and **20 MCP calls per month**, which were used up on 2026-10-09. Don't plan work that depends on Figma. The HTML mockups are the reference.
- Published mockups (private claude.ai artifacts): current combined design https://claude.ai/artifact/BwuSiLCGAxV5fGR86MSmjH · minimal concept https://claude.ai/artifact/SuxRScjnPrCJkpGEdogd7z. The detailed concept lives only in the repo (`docs/design/screens-detailed.html`).
- To preview HTML locally, Chromium and Playwright are available in the cloud container (`/opt/node22/lib/node_modules/playwright`).
