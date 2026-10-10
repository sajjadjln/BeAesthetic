# Tasks and status

_Last updated: 2026-10-10_

## Where we are

| Phase | Status |
|---|---|
| 1. Discovery | ✅ Done. Vision agreed. |
| 2. Spec | ✅ Done (`docs/SPEC.md`). Some items still open, see below. |
| 3. Design | ✅ **Approved 2026-10-10** (round 3, `docs/design/screens.html`); font and accents decided the same day (D47). |
| 4. Architecture | ✅ **Approved 2026-10-10** (`docs/ARCHITECTURE.md`, D31–D45). |
| 5. Build | ⏳ **Next.** No app code exists yet. Done on the user's laptop in Android Studio, not in a cloud session. |

The program itself started on **2026-10-09 (Day 0)**. Week 1 began 2026-10-10. Every week without the app is a week without reminders, so keep moving.

## Next steps

1. **Phase 5, slice 1**, in a session on the user's laptop (Android Studio and the Android SDK are there): create the project (`:domain` + `:app`, app ID `app.beaesthetic`), theme (tokens with 5 accents, Estedad, RTL, both languages), Room with seeded tasks, Today with ticking, day snapshot, minimum day, week dots. See `ARCHITECTURE.md` §14 for the full slice list:
   1. Skeleton + Today.
   2. Reminders: planner, scheduler, receivers, reminder health, Xiaomi/HyperOS setup screen, Reminders screen.
   3. Tasks list + task editor.
   4. Workout plan editor + workout session.
   5. Dashboard (grid, week rows, day detail).
   6. Backup/restore (before any photos are stored for real).
   7. Progress + monthly check-in + baseline entry with past dates + strength tests.
   8. Friday review and plan.
   9. Onboarding.
2. Screens not designed yet (onboarding, check-in, strength test, backup/restore, day detail, notifications, English layouts) are designed inside the slice that needs them, as a quick HTML mockup first.
3. Before slice 1: the user creates the release keystore and stores it safely (`ARCHITECTURE.md` §13).

## Open questions for the user

1. **Bedtime** on normal days and Fridays (sets the Night reminder; default 22:30).
2. Is the **Friday hike** the Friday workout, or a home workout plus the hike?
3. **What time does the user train?** (decides which part of the day the workout tile sits in; default evening 19:00)
4. Keep the three **protein** ticks (breakfast / lunch / dinner)? (Default: yes)
5. **Strength tests** every 2 weeks? Which **leg-health** test?
6. **Beard and haircut** intervals (days); **vitamin D** dose and schedule.
7. **The workout plan** (exercises, sets, rep ranges for Sat / Mon / Wed). The user said they'll paste it. It seeds the Workout plan editor.
8. **Snack list:** 5–10 snacks they can make.
9. Day-level thresholds and the special-day cap (2/week) are defaults. Revisit after 2 weeks of real use.
10. **Persian app name** (launcher label). Until decided, the label is «BeAesthetic» in both languages.
11. **Which Friday is the monthly check-in?** Needed for the check-in schedule. Default: the Friday closest to the 17th of each Jalali month (same spacing as Day 0). The mockup's «جمعه ۱۴ آذر» is sample data: 14 Azar 1405 is a Saturday.

## Backlog (after MVP, from `SPEC.md` §3.2)

- Public release on Cafe Bazaar (far away; see `ARCHITECTURE.md` §15).
- Optional GitHub Actions workflow that runs the tests on push.
- Goals with progress bars inside Progress.
- Before/after photo comparison slider.
- In-app camera with a ghost overlay of the previous photo.
- Meal and snack planner with recipes.
- 5-week phases with new milestones.
- Home-screen widget for Today.
- Optional sync to the user's own .NET backend.

## Housekeeping

- `main` holds all work so far (merged 2026-10-10). If GitHub's default branch is still `claude/trusting-lovelace-e5b7iq`, switch it to `main` in the repo settings.
- The Figma file is out of date; don't update it unless the user upgrades their plan.
