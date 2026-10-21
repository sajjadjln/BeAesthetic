# BeAesthetic

**«پیکر»** (English: BeAesthetic) is a personal Android app for one person (the user) that runs a 23-week body-transformation program from **9 Oct 2026 to 20 Mar 2027** (17 Mehr – 29 Esfand 1405). It tells the user what to do each day, logs workouts and body progress, and reminds reliably on a Xiaomi phone without Google services.

## Read these first, in order

| File | What it holds | Update it when |
|---|---|---|
| `docs/TASKS.md` | **Where we are now**: current phase, next steps, open questions, backlog | Every session that changes status |
| `docs/DECISIONS.md` | Every decision so far, with the reason, including reversals | A decision is made or changed |
| `docs/SPEC.md` | Product spec: vision, scope, rules (scoring, intervals, reminders, backup), data model | Product behavior changes |
| `docs/DESIGN.md` | Design system and the screen-by-screen design that is current | Visual or screen design changes |
| `docs/ARCHITECTURE.md` | Stack, modules, reminders, Room schema, backup, testing, build order | Technical design changes |
| `docs/design/screens.html` + `flows.html` | **The current design**, interactive HTML mockups: the main screens, then every flow (onboarding, check-in, tests, backup, letters, recaps) | Screens change |
| `research/FINAL-REPORT.md` | Market, competitor and gamification research (2026-10-10); the evidence behind D48–D69, and the experiment plan (`research/10`) | New research is done |

If these files disagree, the order of trust is: `DECISIONS.md` (newest entry wins) → `ARCHITECTURE.md` (technical) / `DESIGN.md` (visual) → `SPEC.md`. Then fix the file that is wrong.

## About the user

- A developer with an Android background and a .NET backend background. Reads and reviews code. Explain technical choices with the reasons behind them.
- Prefers **direct, opinionated recommendations** over a menu of equal options. When asking questions, ask 3–5 at a time, mostly multiple choice, mark the recommended option and give a one-line reason.
- Lives in **Iran**: Google Play Services, Firebase push and some APIs are unreliable. The app must not depend on them.
- Phone: **Poco X4 5G on HyperOS** (Xiaomi). It must work on any Android phone, but this phone is the reliability target.
- Plans to publish the app on **Cafe Bazaar** in the future, after the program (D49). For now it's only for them. Don't add release scope to the MVP; just don't hard-code anything personal.
- Works in Persian and English. The app is **Persian only (RTL)**. An English version, if it ever comes, is a separate build, not a language switch (D87).
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
- **Local-first.** No Google Play Services, no Firebase, no analytics or crash SDKs, no accounts, no `INTERNET` permission (D60), no sharing (D63). A local-only usage log is allowed: it never leaves the phone except inside the user's own backup (D59). Installed as an APK, not through the Play Store.
- **Never lose data** (automatic backups) and **never miss a reminder silently** (reminder health check).

## Conventions

- **Language:** Persian only, RTL, Persian digits (D87). All UI strings still live in resources and are never hard-coded, so a separate English build stays possible.
- **Dates:** stored as Gregorian `LocalDate`. Jalali is presentation only, shown first with Gregorian second. The week starts **Saturday**. The day rolls over at **04:00**.
- **Time direction:** in RTL, time flows right → left (dashboard grid and charts).
- **Colors:** the accent = done/progress. The user picks it (green by default, or blue, turquoise, violet, orange). Amber = due or important (written as «موعدش رسیده» / «مهم»), yellow with the orange accent; indigo = special day (sick, travel, off), shifted for blue/violet; gray = gap. Palettes in `DESIGN.md`.
- **Font:** Estedad only, bundled in the APK.
- **Docs:** written in plain English, short sentences. UI copy is in Persian.

## Repository and environment notes

- **`main` is the main branch** (created 2026-10-10 from `claude/trusting-lovelace-e5b7iq`, which has the same history). Start new work from `main`.
- **App code exists from slice 1 on** (2026-10-10): `:domain` (pure Kotlin rules and tests) and `:app` (Android). Build from the command line with Android Studio's JDK: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew check :app:assembleDebug`. `./gradlew check` runs the domain tests, lint, the no-`INTERNET` check and the banned-dependency check.
- **The app is built on the user's laptop in Android Studio** (it has the Android SDK) and installed over USB. Cloud sessions can't download the Android SDK (`dl.google.com` is blocked), so don't create or build the Android project there. Pure Kotlin (`:domain`) tests can run in a cloud session.
- **Figma** file https://www.figma.com/design/1DzXN6iOhAa0PsnLDSjOAg holds the first design-system pass. It is **out of date**. The user's Figma account is on the Starter plan: 3 pages, 1 variable mode per collection, and **20 MCP calls per month**, which were used up on 2026-10-09. Don't plan work that depends on Figma. The HTML mockups are the reference.
- Published mockups (private claude.ai artifacts): current combined design https://claude.ai/artifact/BwuSiLCGAxV5fGR86MSmjH · minimal concept https://claude.ai/artifact/SuxRScjnPrCJkpGEdogd7z · motion demo https://claude.ai/artifact/SdjkRYt5z13an56aZ99huE (`docs/design/motion.html`) · logo board https://claude.ai/artifact/2o9iY6KqvcGFTbhYhFF6j9 (`docs/design/logo.html`) · flows (onboarding, check-in, tests, backup, letters, recaps, English) https://claude.ai/artifact/An8XmUuGo4cWrbHHhWMkom (`docs/design/flows.html`) · tasks v2 (workout card, «هر وقت», simple editor, calendar; D126–D130) https://claude.ai/artifact/Nqghovm1T2ys33Cf6obF4S (`docs/design/tasks-v2.html`) · character board https://claude.ai/artifact/Jf7hbWjUQTf39HRDcVkExZ (`docs/design/character.html`; the rig `docs/design/piker.js` is shared with `screens.html`) · character candidates for the goal-app direction, hoopoe and old magician https://claude.ai/artifact/Dnv8S5qMsxkbMYVCedcFCN (`docs/design/character-candidates.html`; round 2, real-looking, is drawn by `docs/design/candidates-real.js`, round 1, cartoon, by `docs/design/candidates.js`). The detailed concept lives only in the repo (`docs/design/screens-detailed.html`).
- To preview HTML locally, Chromium and Playwright are available in the cloud container (`/opt/node22/lib/node_modules/playwright`).
