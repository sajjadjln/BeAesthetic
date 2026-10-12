# Tasks and status

_Last updated: 2026-10-12_

## Where we are

| Phase | Status |
|---|---|
| 1. Discovery | ✅ Done. Vision agreed. |
| 2. Spec | ✅ Done (`docs/SPEC.md`). Some items still open, see below. |
| 3. Design | ✅ **Approved 2026-10-10** (round 3, `docs/design/screens.html`); font and accents decided the same day (D47). |
| 4. Architecture | ✅ **Approved 2026-10-10** (`docs/ARCHITECTURE.md`, D31–D45). |
| Research | ✅ **Done 2026-10-10** (`research/`, start with `research/FINAL-REPORT.md`). All recommendations accepted except sharing (D48–D69). |
| 5. Build | ⏳ **In progress.** Slices 1–4 built and checked on an emulator (4a and 4b on 2026-10-12). Next: slice 5 (dashboard). |

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

0. **Slice 4b is built (2026-10-12, 0.4.1, D119–D122).** Ladders: the move sheet's «حرکت سخت‌تر» links the next harder variant (chains, no loops). The session shows «پله‌ی ۱ از ۲، بعدی: …». When every planned set hits the top of the range, the summary offers the next rung or «بازه بشه ۹–۱۳؟»; «بله» changes the plan. Round numbers (reps and seconds per ladder, workouts) show in the summary; days shown up as a card on Today. Database v4 (`exercise.harderId`, `round_number`) with a tested migration. Correcting a set no longer jumps to the summary. Checked on an emulator: linked Split squat → Bulgarian; three sets at 12 offered «پله‌ی بعد: Bulgarian؟»; «بله» swapped the move in Monday's plan and the next session showed «پله‌ی ۲ از ۲، آخرین پله»; 600 s of wall sit showed «۱۰ دقیقه Wall sit از روز صفر». The days card is covered by unit tests only (30 days need real time). **Next:** slice 5, the dashboard.
0. **Slice 4a is built (2026-10-12, 0.4.0, D113–D118).** The plan editor (Settings → «برنامه تمرین»): day pills, the workout name, moves with a drag handle, and an exercise sheet (name with suggestions from other days, reps or seconds, sets, range or «حداکثر», kg, per side, target effort, rest, note). The session: the wide workout tile's play button opens it; pre-filled big number, kg chip, effort bar, set circles, «قبلی», «رکورد», dashes to jump between moves, «پایان» and the summary with «پیکر». Any logged set ticks the workout task; closing and reopening the same day continues. Database v3 (exercise, workout, workout_exercise, session, set_log) with a tested migration. Checked on an emulator: the v2 data migrated; a Monday plan with two moves (6 kg per side, and seconds «حداکثر»); 3 sets logged ticked «تمرین» on Today; reopening continued on the second move, also after a reinstall; the summary showed; a week later (debug «+1 week») the sets were pre-filled from last week with «قبلی» and 11 reps showed «رکورد»; dragging a row saved the new order. **For the user:** install 0.4.0, turn on «تمرین روز» on your workout task, enter your 4 days from `docs/program/12-week-program.md`, and log a real session. **Not yet:** ladders, the next-rung offer and round numbers (4b, D113). **Small open points:** the shared − / + stepper puts − on the right in RTL (the mockup puts + on the right), in the task editor too; the play icon isn't mirrored for RTL.
0. **0.3.1 (2026-10-12):** fixes the autostart row flashing «باز کن» and the health line that stayed on «یه نگاه می‌خوان» after checking (D112). The missed reminder on the morning of 12 Oct isn't explained yet: connect the Poco and read the reminder log.
0. **Slice 3 is built (2026-10-11, 0.3.0):** the tasks list (07: filter by part, one-offs first, markers for «مهم», floor, alarm and every-N, an empty state) and the task editor sheet (08: name, 11 icons, part, repeat with weekdays / every N / one-off day, «بعد از…» with suggestions from anchors in use, the «زنگ» card with time and days, «مهم», «روز حداقلی», delete = archive). Saving re-plans reminders and rewrites today's snapshot (D107–D111). Checked on an emulator: moved «تمرین» to Morning, added Friday and a 09:00 alarm; the alarm was planned for the next training day (Mon 12 Oct 09:00) with Sat/Mon/Wed/Fri; a one-off for tomorrow saved and showed «فردا · صبح»; deleting it archived the row and kept today's snapshot right; an empty name shows the amber message at the top. **For the user:** install 0.3.0 and move your workout to Morning with Friday added (D102); add your own tasks (creatine, water, the Daily 5 mobility). **Next:** slice 4 (workout plan editor + session, D103–D105).
0. **The workout plan arrived (2026-10-11):** `docs/program/12-week-program.md`. It shaped D100–D106: the app starts empty, training is in the morning, sets log optional kg, the plan editor holds range or «حداکثر», per side, target effort, rest and a note, and strength tests are user-defined and monthly.
0. **Slice 2 is built (2026-10-11).** Today complete: ring, week dots (before-app dotted, future outlined), minimum day (leaf), the chapter line and, in the last chapter, the countdown, the chapter-start card with the letter offer, the first-letter offer, the letter sheet and the letter's opening card, tick motion (fill from the touch point, 100% moment), «پیکر» with day poses, chapter outfits and the 7-day line picker. Reminders complete: follow-ups with «انجام شد», task alarms (scheduled; set them in slice 3's editor), health + Today banner + the watchdog's once-a-day warning, device-off marking at boot, the absence rule, Settings, «یادآورها», «گوشی» (Xiaomi intents) and «ظاهر». Database v2 with a tested migration. Checked on an emulator: the v1 database migrated with its ticks; the Night block and both follow-ups fired; «انجام شد» ticked the task on Today; the 1 Azar and 1 Esfand cards and the countdown showed with the debug clock. **Checked on the Poco (2026-10-11, release 0.2.1):** the signed release installs and runs; the test notification arrives and vibrates; the «گوشی» rows open the real settings and show their real state (D99). **Next:** a few days of real use, then slice 3 (tasks list and editor).
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
6. ~~Release keystore~~ → created 2026-10-11 (`~/peykar-release.jks`); `scripts/save-signing.sh` saved it for `scripts/release.sh`. Keep a backup of the file and its password.
7. **For the user, before ~6 Nov (no app needed):** take the 2nd photo set with the same setup as Day 0: same spot and background, same light (no flash), phone at the same height and distance (timer or stand), same time of day (before breakfast), same clothes, relaxed pose. Write the setup down once.
8. After the first 2–3 weeks with the app: that's the baseline for the experiments in `research/10-experiment-backlog.md` (D66). The first ones (anchors, notification wording, in-session record) start around week 5–6.

## Open questions for the user

1. ~~Bedtime~~ → 23:30, Night block at 23:00 (D88).
2. ~~Friday hike~~ → Friday is a training day; the hike is a hobby (D102).
3. ~~Training time~~ → evening (D88).
4. ~~Protein ticks~~ → the user adds their own tasks (D100).
5. ~~Strength tests~~ → user-defined, with the monthly check-in (D106).
6. ~~Beard, haircut, vitamin D~~ → the user adds their own tasks (D100).
7. ~~The workout plan~~ → pasted 2026-10-11 (`docs/program/12-week-program.md`); the user enters it in the editor (slice 4), nothing seeded (D100, D105).
8. ~~Snack list~~ → not seeded (D100).
9. Day-level thresholds and the special-day cap (2/week) are defaults. Revisit after 2 weeks of real use.
10. ~~Persian app name~~ → **«پیکر»** (D79). English stays "BeAesthetic". **Logo:** ✅ the cartoon sticker (D81), files in `docs/design/logo/`; board (`docs/design/logo.html`, https://claude.ai/artifact/2o9iY6KqvcGFTbhYhFF6j9) (D78, D80). **Character:** the sticker guy lives in the app as «پیکر», a hype gym buddy (D82–D84).
11. ~~Check-in Friday~~ → the Friday closest to the 17th of each Jalali month (D101).

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
