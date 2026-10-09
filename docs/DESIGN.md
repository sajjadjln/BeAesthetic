# BeAesthetic: Design System (Phase 3, draft for approval)

Figma file: https://www.figma.com/design/1DzXN6iOhAa0PsnLDSjOAg

- **Design System** page: tokens, text styles and components.
- **Screens** page: mockups (01 Today, 02 Wall).

HTML mockups of all screens: `docs/design/screens.html` (open it in a browser; the checklists and controls work).

## Visual direction: "Quiet strength"

- **Dark first.** You use the app at 06:45 and 22:30, so the dark theme is the main one. The light theme uses the same color names (see Tokens).
- **Green is the only bright color.** It means "you did it" and is used for the Dashboard grid, completed checks and personal records. Everything else stays neutral graphite, so green always means progress.
- **No red, anywhere.** Missed = gray. Needs attention = amber (for example "due"). Special days (sick, travel, off) = indigo. This follows the "gaps, not failures" principle.
- **Calm, dense, one-handed.** Cards on a dark canvas, 48dp-high tap targets, the main action at the bottom of a card, and nothing decorative.
- **Persian first, right-to-left.**
  - Layouts are mirrored, so the check, the icon and the title sit on the right.
  - **Time flows right → left.** On the Dashboard, week 1 is the right-most column. Charts start on the right too.
  - Saturday is the first day of the week.

## Tokens

**Starter-plan constraint.** Your Figma plan allows only one mode per variable collection. So the semantic colors are two collections with identical names, `Theme · Dark` and `Theme · Light`. Both point to the same `Primitives`. In code this becomes one `ColorScheme` with dark and light values.

| Token | Dark | Light | Used for |
|---|---|---|---|
| bg/canvas | graphite/950 `#0E1110` | graphite/50 `#F6F7F6` | Screen background |
| bg/surface | graphite/900 `#151918` | white | Cards |
| bg/surface-2 | graphite/850 `#1C211F` | graphite/100 `#EDF0EE` | Secondary button, neutral chip, photo slots |
| border/default | graphite/800 `#262C2A` | graphite/200 `#DCE1DE` | Dividers, outlines |
| text/primary | graphite/100 `#EDF0EE` | graphite/950 | Titles, labels |
| text/secondary | graphite/400 `#909A96` | graphite/500 `#6B7571` | Supporting text, done items |
| text/tertiary | graphite/500 | graphite/400 | Captions, axis labels |
| accent/default | green/500 `#34C47A` | green/700 `#1D8A55` | Primary button, completed check |
| accent/subtle / on-subtle | green/950 / green/400 | green/100 / green/800 | Positive chips, active tab |
| wall/level-0…4 | graphite/800 → green/900 → green/800 → green/600 → green/400 | graphite/200 → green/200 → green/300 → green/600 → green/800 | Dashboard grid intensity |
| wall/special | indigo/600 `#5560C9` | indigo/400 | Sick, travel and off days |
| notice/subtle / on-subtle | amber/900 / amber/400 | amber/100 / amber/700 | "Due" items, reminder-health warnings |

- **Spacing:** 4 · 8 · 12 · 16 · 24 · 32.
- **Corner radius:** cell 6 · sm 8 · md 12 · lg 16 · xl 24 · full.

## Typography

**Vazirmatn** (open source, SIL OFL) is the only typeface:

- It covers both Persian and Latin, so mixed text like "Mon 9 Nov 2026 · هفته ۵" stays consistent.
- It works with system Persian digits.
- It ships inside the APK, so nothing is fetched from Google Fonts at runtime.

| Style | Size / weight / line-height |
|---|---|
| Display | 40 / ExtraBold / 130% |
| Headline | 24 / Bold / 150% |
| Title | 18 / Bold / 150% |
| Subtitle | 16 / SemiBold / 160% |
| Body | 15 / Regular / 170% |
| Body Strong | 15 / Medium / 170% |
| Label | 13 / Medium / 160% |
| Caption | 12 / Regular / 160% |
| Overline | 11 / SemiBold / 150% |
| Number | 28 / Bold / 120% |

Persian text needs more line-height than Latin. The values above are tuned for Vazirmatn.

## Components (in Figma, bound to tokens)

| Component | Variants / properties | Notes |
|---|---|---|
| Icon/* (23) | stroke icon, 24px | Today, Wall, Proof, Week, dumbbell, shield, sunrise/sun/sunset/moon, trophy, camera, … |
| Check | State = Todo / Done | Round. Done = accent fill + check mark |
| Chip | Tone = Accent / Special / Notice / Neutral; Label | Status labels; never red |
| WallCell | Level = 0…4 / Special / Future / Today × Size = Large / Small | One day on the Dashboard grid |
| ChecklistItem | State = Todo / Done / Due; Label, Sub, Show sub, Non-negotiable | The whole row is the tap target. Done items stay readable (no strike-through) |
| BlockHeader | State = Current / Upcoming / Complete; Title, Time, Progress, Icon | Morning, Midday, Evening, Night |
| Button | Style = Primary / Secondary / Ghost; Label | 48dp tall |
| NavItem, BottomNav | Active; Label, Icon | 4 tabs in right-to-left order: Today · Dashboard · Proof · Week |
| StatCard | Label, Value, From (plus a delta chip) | "Since Day 0" proof |
| Banner | Text, Action | Gentle messages (welcome back, reminder health, backup age) |

## Screens

| # | Screen | Status |
|---|---|---|
| 01 | Today (dark) | ✅ In Figma |
| 02 | Wall (dark) | ✅ In Figma (week labels clipped there) and in HTML |
| 03 | Proof (dark) | ✅ HTML |
| 04 | Workout session (dark) | ✅ HTML |
| 05 | Friday ritual (review → plan) | ✅ HTML |
| 06 | Today (light) | ✅ HTML |
| 07 | Today (English, left-to-right) | ✅ HTML |

## Design decisions to confirm

1. Dark theme by default, with light available, following the system setting.
2. Green as the only accent color. No red, amber for "due", indigo for special days.
3. Time flows right → left on the Dashboard and in charts (consistent with right-to-left reading).
4. Vazirmatn as the only font, shipped inside the APK.
5. The Workout item gets its own card on Today, with last session's numbers and today's target shown before you start.
6. The minimum day is a low-key ghost button at the bottom of Today ("Hard day? Minimum day"), never a pop-up.

## Round 2 changes (after review)

- "Wall" (دیوار) is renamed **Dashboard (داشبورد)**.
- No weekly focus anywhere.
- The minimum day is a two-way switch at the top of Today: **روز کامل / روز حداقلی**.
- Workout: only the last session's numbers are shown ("دفعه قبل شنا: ۱۲، ۱۲، ۱۰"), with no "today's target"; each set is pre-filled with last time's number. The log button just says **ثبت**.
- Snacks are called **میان‌وعده**. In the Friday plan each day is a drop-down row that opens a free-text note (it can hold a whole recipe). The food is not shown on the closed row.
- Much less helper text inside the app.
- Fixed: tappable rows had lost their side padding, so checks and chips touched the card edge (for example "کوتاه کردن ناخن").
- New screens: **Settings** (gear on Today), **Tasks & routines** (with one-off tasks), **Task editor** (repeat, own reminder time, non-negotiable, minimum day) and **Reminders & alarms**.
