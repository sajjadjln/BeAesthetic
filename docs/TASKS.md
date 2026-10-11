# Tasks and status

_Last updated: 2026-10-10_

## Where we are

| Phase | Status |
|---|---|
| 1. Discovery | ✅ Done. Vision agreed. |
| 2. Spec | ✅ Done (`docs/SPEC.md`). Some items still open, see below. |
| 3. Design | ✅ **Approved 2026-10-10** (round 3, `docs/design/screens.html`); font and accents decided the same day (D47). |
| 4. Architecture | ✅ **Approved 2026-10-10** (`docs/ARCHITECTURE.md`, D31–D45). |
| Research | ✅ **Done 2026-10-10** (`research/`, start with `research/FINAL-REPORT.md`). All recommendations accepted except sharing (D48–D69). |
| 5. Build | ⏳ **In progress.** Slices 1–2 built and checked on an emulator (2026-10-10, 2026-10-11). Next: slice 3. |

The program itself started on **2026-10-09 (Day 0)**. Week 1 began 2026-10-10. Every week without the app is a week without reminders, so keep moving.

## Dates that matter for the build

| Date | What | Needs |
|---|---|---|
| ~Fri 6 Nov 2026 | 2nd photo set (Aban check-in, if the default rule in Q11 holds) | The photo checklist below (no app needed) |
| 22 Nov | Chapter ② Azar starts | Slice 2 (chapter line) |
| ~Fri 11 Dec | 3rd photo set (Azar check-in) | Ghost-overlay camera (slice 7) |
| **19 Dec** | **Halfway recap must be ready** (Yalda = 21 Dec, week 11) | Slice 8b |
| ~week 8 (28 Nov) | Widget target; reminder fade offers can start | Slice 10 |
| ~week 18 (late Feb 2027) | After-the-finish mode ready | Slice 10 |
| 20 Mar 2027 | Finish line, eve of Nowruz | Finish recap |

## Next steps

0. **Slice 2 is built (2026-10-11).** Today complete: ring, week dots (before-app dotted, future outlined), minimum day (leaf), the chapter line and, in the last chapter, the countdown, the chapter-start card with the letter offer, the first-letter offer, the letter sheet and the letter's opening card, tick motion (fill from the touch point, 100% moment), «پیکر» with day poses, chapter outfits and the 7-day line picker. Reminders complete: follow-ups with «انجام شد», task alarms (scheduled; set them in slice 3's editor), health + Today banner + the watchdog's once-a-day warning, device-off marking at boot, the absence rule, Settings, «یادآورها», «گوشی» (Xiaomi intents) and «ظاهر». Database v2 with a tested migration. Checked on an emulator: the v1 database migrated with its ticks; the Night block and both follow-ups fired; «انجام شد» ticked the task on Today; the 1 Azar and 1 Esfand cards and the countdown showed with the debug clock. **Not yet checked on the Poco:** the Xiaomi rows (autostart, battery, recents) only show on a Xiaomi. **Next:** the user tries slice 2 on the phone, then slice 3 (tasks list and editor).
0. **Slice 1 is built (2026-10-10).** What works: Room with the seeded routine, basic Today (header with week, part-of-day switcher, tiles that tick with the accent fill and a light vibration, «مهم» and «موعدش رسیده»), the day snapshot, the reminder core (planner, `setAlarmClock`, receivers for fire/boot/update/time change, the 6-hourly watchdog, block notifications with anchors in Persian, the reminder log), the usage log, `installDate`, the no-`INTERNET` and banned-dependency checks, the launcher and notification icons. Checked on an emulator: ticks survive a force-stop; the test alarm fires on time with the screen off; the 23:00 Night block arrived as «شب · ۳ کار» with anchors. **Next:** the user installs it on the Poco, sets Autostart and battery "No restrictions" by hand (the setup screen is slice 2), and lives with it for a few days. Then slice 2.
1. **Phase 5, slice 1**, in a session on the user's laptop (Android Studio and the Android SDK are there). Slice 1 now includes the **reminder core** (D50). Full list in `ARCHITECTURE.md` §14:
   1. Skeleton + basic Today + reminder core (planner, scheduler, block notifications with anchors, reminder log) + usage log + `installDate` + no-`INTERNET` check.
   2. Today complete (ring, dots, minimum day, chapter line + chapter-start card with the letter offer, last-chapter countdown, tick feedback, comeback) + reminders complete (follow-ups, task alarms, health, Xiaomi setup, Reminders screen).
   3. Tasks list + task editor (with the «بعد از…» anchor).
   4. Workout plan editor + workout session (in-session «رکورد», ladders + next-rung offer, round numbers).
   5. Dashboard (with «قبل از اپ» cells).
   6. Backup/restore (before any photos are stored for real).
   7. Progress (ladders, lifetime totals) + check-in with the ghost camera and the reveal + baseline with workout backfill + strength tests with the guess + program line + milestones.
   8. Friday review and plan (short path, comebacks, best routines, «روز ذخیره», identity sentence).
   8b. Halfway recap, **by 19 Dec**.
   9. Onboarding.
   10. Later: widget, reminder fade offer, finish recap + after-the-finish mode.
2. All screens are designed (`screens.html`, `flows.html`, D85). The app is Persian only (D87).
3. **Flows** (D85): look through https://claude.ai/artifact/An8XmUuGo4cWrbHHhWMkom (onboarding, check-in, strength test, backup, letters, recaps) and say what to change.
4. **Character «پیکر»** (D82–D84): look at the board (https://claude.ai/artifact/Jf7hbWjUQTf39HRDcVkExZ) and the Today in `screens.html`, and say which poses, lines or outfits feel off. The Today row and the line picker are built in slice 2; each big moment comes with its slice.
5. **Motion demo** (D76): try `docs/design/motion.html` on the phone and say what feels too much or too little before slice 1 (https://claude.ai/artifact/SdjkRYt5z13an56aZ99huE).
6. Before slice 1: the user creates the release keystore and stores it safely (`ARCHITECTURE.md` §13).
7. **For the user, before ~6 Nov (no app needed):** take the 2nd photo set with the same setup as Day 0: same spot and background, same light (no flash), phone at the same height and distance (timer or stand), same time of day (before breakfast), same clothes, relaxed pose. Write the setup down once.
8. After the first 2–3 weeks with the app: that's the baseline for the experiments in `research/10-experiment-backlog.md` (D66). The first ones (anchors, notification wording, in-session record) start around week 5–6.

## Open questions for the user

1. ~~Bedtime~~ → 23:30, Night block at 23:00 (D88).
2. Is the **Friday hike** the Friday workout, or a home workout plus the hike?
3. ~~Training time~~ → evening (D88).
4. Keep the three **protein** ticks (breakfast / lunch / dinner)? (Default: yes)
5. **Strength tests** every 2 weeks? Which **leg-health** test?
6. **Beard and haircut** intervals (days; seeded as 7 and 30, D90); **vitamin D** dose and schedule.
7. **The workout plan** (exercises, sets, rep ranges for Sat / Mon / Wed). The user said they'll paste it. It seeds the Workout plan editor. With it: the **ladder rungs** for each move (D70). Default seeds: push-up = knee → standard → diamond → archer; pull-up = dead hang → negative → band-assisted → strict.
8. **Snack list:** 5–10 snacks they can make.
9. Day-level thresholds and the special-day cap (2/week) are defaults. Revisit after 2 weeks of real use.
10. ~~Persian app name~~ → **«پیکر»** (D79). English stays "BeAesthetic". **Logo:** ✅ the cartoon sticker (D81), files in `docs/design/logo/`; board (`docs/design/logo.html`, https://claude.ai/artifact/2o9iY6KqvcGFTbhYhFF6j9) (D78, D80). **Character:** the sticker guy lives in the app as «پیکر», a hype gym buddy (D82–D84).
11. **Which Friday is the monthly check-in?** Needed for the check-in schedule. Default: the Friday closest to the 17th of each Jalali month (same spacing as Day 0). The mockups use that default: «جمعه ۲۰ آذر» (11 Dec).

12. ~~Tape sites~~ → waist, chest, arm (right, flexed), thigh (right), shoulders (D85, confirmed).

## Backlog (after MVP, from `SPEC.md` §3.2)

- Public release on Cafe Bazaar: a **future goal, after the program** (D49; `ARCHITECTURE.md` §15, `research/07`).
- Optional GitHub Actions workflow that runs the tests on push.
- Goals with progress bars inside Progress (milestones, D65, are the simple version in the MVP).
- Meal and snack planner with recipes.
- Optional sync to the user's own .NET backend.

Moved into the build by the research: the ghost-overlay camera (slice 7), "5-week phases" (now chapters, D51), the widget (slice 10, D67). The before/after slider is now part of the check-in reveal (slice 7, D75).

## Housekeeping

- `main` holds all work so far (merged 2026-10-10). If GitHub's default branch is still `claude/trusting-lovelace-e5b7iq`, switch it to `main` in the repo settings.
- The Figma file is out of date; don't update it unless the user upgrades their plan.
