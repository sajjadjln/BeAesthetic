# Tasks and status

_Last updated: 2026-10-10_

## Where we are

| Phase | Status |
|---|---|
| 1. Discovery | ✅ Done. Vision agreed. |
| 2. Spec | ✅ Done (`docs/SPEC.md`). Some items still open, see below. |
| 3. Design | 🟡 **Round 3 delivered** (`docs/design/screens.html`). Waiting for the user's final OK. |
| 4. Architecture | ⏳ Next |
| 5. Build | ⏳ Not started. No app code exists yet. |

The program itself started on **2026-10-09 (Day 0)**. Week 1 began 2026-10-10. Every week without the app is a week without reminders, so keep moving.

## Next steps

1. **Ask the user to approve design round 3**, or collect final changes. Then summarize Phase 3 decisions (the user asked for a summary at the end of each phase).
2. **Phase 4: Architecture.** Propose the stack with reasons. The user's default expectation: native Kotlin + Jetpack Compose, Room, local notifications (AlarmManager/WorkManager), no Google services, optional sync later. Cover:
   - Module structure; DI choice (Hilt needs no Google services; or manual DI / Koin).
   - Alarm strategy on Xiaomi: `setAlarmClock` vs `setExactAndAllowWhileIdle`, `USE_EXACT_ALARM` (allowed because the app is sideloaded), boot/time-change receivers, reminder-health log.
   - Jalali date library (or a small in-house converter with tests).
   - Room schema from `SPEC.md` §9 (update it for D25–D28: one-off tasks, task alarm weekdays, effort 1–10, monthly check-ins).
   - Backup: zip of a JSON export + DB copy + photos, written via the Storage Access Framework to a Syncthing folder; restore flow.
   - Photo storage in app-private storage.
   - Testing strategy: pure Kotlin unit tests for scoring, interval due dates, reminder scheduling and Jalali conversion.
   - Build and distribution: signed APK, versioning, how the user installs updates.
3. **Phase 5: Build** in vertical slices. Proposed order (confirm in Phase 4):
   1. Project skeleton, theme (tokens + Vazirmatn + RTL), Today with seeded tasks, local persistence.
   2. Reminders for parts of the day + Xiaomi setup screen + reminder health.
   3. Tasks list + task editor (repeat rules, alarms, important, minimum day).
   4. Workout plan editor + workout session.
   5. Dashboard (grid + week rows).
   6. Backup/restore (before any photos are stored for real).
   7. Progress + monthly check-in + baseline entry with past dates.
   8. Friday review and plan.

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

## Backlog (after MVP, from `SPEC.md` §3.2)

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
