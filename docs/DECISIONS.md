# Decision log

Newest decisions override older ones. Each entry: what was decided, why, and its status.
Status: **Final** (agreed by the user) · **Default** (my choice, not yet confirmed; the user may override) · **Superseded** (replaced by a later entry).

## Phase 1: Discovery (2026-10-09)

| # | Decision | Why | Status |
|---|---|---|---|
| D1 | The app's job: a daily checklist first, plus weekly planning, a workout log, and visible proof of progress. | The user's failure modes are forgetting and quitting; a daily checklist attacks them every day. | Final |
| D2 | Today shows the whole day split into Morning / Midday / Evening / Night; the current part is in focus. | Routines naturally cluster at these times. | Final |
| D3 | Reminders are gentle by default, with **one** follow-up for tasks the user marks as important ("non-negotiable"). | If everything nags, the app gets muted; if nothing nags, brushing and skincare get forgotten. | Final |
| D4 | Day progress is shown as green intensity by % done. Missed = gray, **never red**. Special days (sick, travel, off) get their own color, max 2 per week. | The user's top risk is "a bad week makes me avoid looking". Red would cause exactly that. | Final (cap of 2 is Default) |
| D5 | A "minimum day" floor (e.g. brush, creatine, 10 push-ups) still counts as showing up. | Keeps the user in the game on bad days without a streak to lose. | Final |
| D6 | Top abandonment risks: "no visible change" and "a bad week makes me avoid looking". Fixes: strength numbers as early proof; gray gaps; weekly fresh start; welcome-back without listing misses. | Stated by the user. | Final |
| D7 | Weekly ritual on **Friday** (the user's weekend): review the week, then plan the next. Weeks run Saturday → Friday. | The user plans on their weekend; Friday evening closes the week. | Final |
| D8 | Language: fully bilingual (Persian + English) with RTL. **Persian is the default.** Jalali date first with Gregorian underneath. Digits follow the language. | User choice (I had recommended English-only first). | Final |
| D9 | No dependency on Google Play Services. Local exact alarms. APK install. Xiaomi is the reliability target, any Android must work. | The user lives in Iran and uses a Xiaomi. | Final |
| D10 | Backup: automatic weekly zip into a folder synced to the PC with **Syncthing**; plus manual export/import. | Photos and history must never be lost; works without foreign servers. | Final |
| D11 | Cut: calorie/protein tracking, generic calendar, full task manager. | Don't serve the goal; friction. | Final (see D25 for one-off tasks) |
| D12 | Protein stays only as three checklist items (with breakfast / lunch / dinner). | Keeps the habit visible with zero logging. | Default, not confirmed |
| D13 | Repeating routines (nails, shower, shampoo, haircut) are counted **from the last time done**. A late item only counts against the day it was first due. | Matches real life; lateness never piles up. | Final |
| D14 | Reminder times are fixed clock times with separate **Normal** and **Friday** profiles. User wakes at 06:45. | Predictable and easy to trust. | Final |
| D15 | Vision paragraph agreed (see `SPEC.md` §1). | | Final |

## Phase 2: Spec (2026-10-09)

| # | Decision | Why | Status |
|---|---|---|---|
| D16 | Spec written in `docs/SPEC.md`. The user moved on to design ("create the design"), which was taken as approval; open items keep their defaults. | | Final |
| D17 | Day rolls over at 04:00; edit window is today + yesterday. | Late-night brushing counts for the right day; history stays honest. | Default |
| D18 | Day level thresholds: 0 nothing · 1 anything or floor done · 2 ≥ 50% · 3 ≥ 80% · 4 = 100%. Tune after 2 weeks. | | Default |
| D19 | After 3 days without opening the app, reminders reduce to Morning only. | Less noise when away; easy return. | Default |
| D20 | Strength tests every 2 weeks (Saturday, before the session). | Early, visible wins. | Default, not confirmed |

## Phase 3: Design (2026-10-09 → 2026-10-10)

| # | Decision | Why | Status |
|---|---|---|---|
| D21 | Visual direction "quiet strength": dark-first, green as the only accent, amber for due/important, indigo for special days, Vazirmatn as the only font. | Used at 06:45 and 22:30; green must always mean progress. | Final |
| D22 | Figma was used for the first design-system pass, then dropped: the Starter plan's MCP limit (20 calls/month) ran out. Design continues in **HTML mockups**. | Free, fast, viewable on the phone. | Final |
| D23 | The user rejected an external design tool (Stitch) result in favour of this design. | | Final |
| D24 | Round 2 changes: "Wall" (دیوار) renamed **Dashboard (داشبورد)**; weekly focus (تمرکز هفته) removed; snacks are called **میان‌وعده** and stored as a free-text note per day that opens like a drop-down; less helper text everywhere. | User feedback. | Final |
| D25 | **Where tasks/reminders are set:** a Settings screen (gear on Today) → Tasks, Reminders, Workout plan, Backup, Language, Phone setup. One-off tasks are back in scope as "repeat once". Tasks can have their own alarm time. | User asked where alarms and tasks are configured. | Final |
| D26 | Workout: no "today's target". Each set is pre-filled with last session's number; only "last time: 12, 12, 10" is shown. The log button just says «ثبت». | User feedback. | Final (supersedes the "target" idea in D-spec §6.4) |
| D27 | Round 3, the **combined design** (current): | User picked per screen. | Final pending one last look |
|  | · Today = minimal: day ring, 7 week dots, part-of-day icon switcher, big icon tiles. Due task → amber outline + «موعدش رسیده». Important task → gold outline + «مهم». Leaf button = minimum day. | | |
|  | · Bottom nav = **icons only**, 4 tabs: Today · Dashboard · Progress · Week. | | |
|  | · Dashboard = detailed grid + week rows. «میانگین هفته‌ها» removed. | | |
|  | · Progress = detailed (stat cards, chart, weight, photos). | | |
|  | · Workout = minimal big-number counter. Effort = a **1–10 seek bar** labelled «درجه سختی». | | |
|  | · Week (Friday) = detailed review + plan. | | |
|  | · Settings = minimal 6 tiles. Tasks = detailed list. Task editor = minimal + alarm (time + which weekdays it rings). Reminders = detailed. | | |
|  | · New screen: **Workout plan editor** (برنامه تمرین). | | |
| D28 | Photo and tape-measure check-ins are **monthly** (7 sets: Day 0, then one per Jalali month, plus the finish line), not every 4 weeks. | User request. | Final |
| D29 | The GitHub-style grid is no longer required on Today; it lives only on the Dashboard. | User: "you don't need to follow everything I said". | Final |

## Superseded

- Weekly focus sentence (Phase 2) → removed in D24.
- "Today's target" in workouts → removed in D26.
- Effort as 4 words (Easy/Good/Hard/Max) → replaced by the 1–10 seek bar in D27.
- Photos every 4 weeks → monthly in D28.
- Minimum day as a bottom ghost button, then as a top switch → now a leaf icon button on Today (D27).
- Thursday planning (first draft) → Friday (D7).
