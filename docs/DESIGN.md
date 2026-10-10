# BeAesthetic: Design

**Current design:** `docs/design/screens.html` (interactive; open it in a browser).
Published copy: https://claude.ai/artifact/BwuSiLCGAxV5fGR86MSmjH

**Open exploration: fonts and accent color** (2026-10-10): `docs/design/type-and-color.html`, published at https://claude.ai/artifact/WFBdvYr9iUavDkA1ifyZyE. Today in six Persian fonts and four accent colors, plus the «ظاهر و زبان» screen where the user picks the color. Nothing changes in the tokens below until the user picks (D46).

Kept for reference only:
- `docs/design/screens-minimal.html`: minimal concept (https://claude.ai/artifact/SuxRScjnPrCJkpGEdogd7z)
- `docs/design/screens-detailed.html`: detailed concept (round 2)
- Figma https://www.figma.com/design/1DzXN6iOhAa0PsnLDSjOAg: first design-system pass. **Out of date** (old names, old Today).

The history of how we got here is in `docs/DECISIONS.md` (D21–D29).

## Visual direction: "quiet strength"

- **Dark first**, with a light theme. The app follows the system theme.
- **Green is the only accent.** It means done or progress.
- **No red anywhere.** Gray = a gap. Amber = due or important, always with words («موعدش رسیده», «مهم»). Indigo = special day (sick, travel, off).
- **Visual over text.** Icons, rings, big numbers. Minimal helper text.
- **Persian first, RTL.** Mirrored layouts; time flows right → left; the week starts Saturday.
- One-handed: 44–48dp tap targets, the main action at the bottom.

## Tokens

| Token | Dark | Light | Used for |
|---|---|---|---|
| bg | `#0E1110` | `#F6F7F6` | Screen background |
| surface | `#151918` | `#FFFFFF` | Cards, tiles |
| surface-2 | `#1C211F` | `#EDF0EE` | Inputs, secondary controls |
| border | `#262C2A` | `#DCE1DE` | Dividers, outlines |
| text-1 / text-2 / text-3 | `#EDF0EE` / `#909A96` / `#6B7571` | `#0E1110` / `#5F6965` / `#8A948F` | Text levels |
| accent | `#34C47A` | `#1D8A55` | Primary buttons, done state, ring |
| accent-subtle / on-subtle | `#0F2E1F` / `#5BD994` | `#E4F7EC` / `#176B43` | Done tiles, positive chips |
| grid level 0…4 | `#262C2A` `#114029` `#176B43` `#22A565` `#5BD994` | `#DCE1DE` `#C4F0D6` `#93E6B6` `#22A565` `#176B43` | Dashboard day squares, week dots |
| special / subtle / on-subtle | `#5560C9` / `#1F2448` / `#8C95EA` | `#8C95EA` / `#D3D7F8` / `#3E479E` | Special days, minimum day |
| amber / subtle | `#E8B04B` / `#36280D` | `#B07610` / `#FBEFD6` | Due and important |

- **Spacing:** 4 · 8 · 12 · 16 · 24 · 32.
- **Radius:** 6 (grid cells) · 12 (controls) · 16 (cards) · 20 (tiles) · 28 (sheets) · full.
- **Font:** Vazirmatn (SIL OFL), bundled in the APK. Persian digits in Persian UI.

| Style | Size / weight |
|---|---|
| Display number | 52–96 / ExtraBold (ring center, workout counter) |
| Headline | 24–26 / ExtraBold |
| Title | 18 / Bold |
| Subtitle | 16 / SemiBold |
| Body | 15 / Regular–Medium |
| Label | 13 / Medium |
| Caption | 12 / Regular |

## Navigation

- Bottom bar with **icons only**, 4 tabs, RTL order from the right: **Today** (check-circle) · **Dashboard** (grid) · **Progress** (trend line) · **Week** (calendar). The active tab has a small green dot.
- **Settings** opens from the gear on Today.
- Full-screen flows: Workout session, Check-in, Friday ritual, Onboarding, Reliability setup, Backup/restore.

## Screens (current)

### 01 امروز (Today)
- Header: weekday (large), "۱۸ آبان · هفته ۵", a **leaf** button (minimum day) and a **gear** (settings).
- **Ring**: tasks done today / total (e.g. ۳ از ۱۲). Under it, **7 week dots** (Saturday on the right), colored by the day level; today is outlined.
- **Part-of-day switcher**: 4 icons (sunrise · sun · sunset · moon). A tiny green dot under an icon means that part is complete.
- **Tiles** (2 columns): an icon + one or two words. Tap toggles done (green fill + check).
  - Important task: **gold outline** and the word «مهم».
  - Due interval task: **amber outline** and a chip «موعدش رسیده».
  - Workout: a wide tile with a play button that opens the Workout session.
- **Minimum day** (leaf on): the part-of-day switcher hides, «روز حداقلی» shows, and only the floor tiles appear.

### 02 داشبورد (Dashboard)
- Title and the program range.
- Summary: «روز حضور» (e.g. ۳۰ از ۳۱) and «روز ویژه». **No weekly average.**
- «کل مسیر»: a 23-column × 7-row grid of small squares (week 1 on the right, Saturday on top); Jalali month labels below.
- Legend: less → more, and special.
- «هفته به هفته»: one row per week (newest on top): label + date range, 7 squares, and a score chip («جاری» for the current week).

### 03 پیشرفت (Progress)
- Monthly check-in card: «چک‌این ماهانه: جمعه ۱۴ آذر · وزن، متر، ۳ عکس» and the days left.
- 2×2 stat cards since Day 0: pull-ups, push-ups, plank, waist (value, delta chip, starting value).
- Pull-up chart (time right → left; dashed segment = the next test) with a «رکورد» chip.
- Weight card with a sparkline.
- «عکس‌های ماهانه»: one row per month (front / side / back), "2 of 7".

### 04 تمرین (Workout session)
- Close button, exercise-progress dashes (e.g. 2 of 6).
- Exercise name; set circles (done sets show reps; the current set follows the counter).
- **Huge number** with − / +, pre-filled from last time. Under it: «قبلی ۱۲ · ۱۲ · ۱۰».
- **Seek bar 1–10** with the label «درجه سختی» and the value.
- A big round ✓ button logs the set (ثبت).

### 05 هفته (Friday review and plan)
- Steps: مرور (done) → برنامه.
- Review card: the week's 7 squares, score chip, wins (records, best routines). Wins only, no misses.
- «روزهای ویژه»: 7 day chips; tap to mark special (max 2; a third tap is refused).
- «میان‌وعده‌ها»: one row per day; closed rows show only the day and a dot (filled = has a note). A row opens to a free-text box (can hold a whole recipe).
- Button «شروع هفته ۵». Finishing runs the weekly backup.

### 06 تنظیمات (Settings)
- «یادآورها سالم‌اند» health banner.
- Six tiles: کارها · یادآورها · برنامه تمرین · پشتیبان · زبان · گوشی (Xiaomi battery/autostart setup).

### 07 کارها (Tasks)
- Filter: همه / صبح / ظهر / عصر / شب.
- Sections: one-off tasks, then each part of the day. Rows show the name and the repeat rule; markers: «مهم» chip, leaf (in minimum day), bell (own alarm), repeat icon (every N days).
- Floating «کار جدید» button.

### 08 کار جدید / ویرایش (Task editor, bottom sheet)
- Name, icon picker (6 icons), part of day (4 icons), repeat (هر روز / روزهای خاص / هر N روز / یک بار).
- **«زنگ»** card with a switch: the time (large) and **7 day circles** to choose which weekdays the alarm rings.
- Toggles: «مهم» (one extra reminder), «روز حداقلی» (part of the floor).
- «ذخیره».

### 09 یادآورها (Reminders)
- Health card with an «آزمایش» (test) button.
- Profile switch: روزهای عادی / جمعه.
- One time + switch per part of the day.
- «زنگ‌های کارها»: every task alarm (name, repeat, time, switch).
- Follow-up delay for important tasks (45 min) and the Friday review time.

### 10 برنامه تمرین (Workout plan editor)
- Day picker: 7 days; training days show their workout letter (A, B, C) and Friday shows «کوه».
- Workout name field (e.g. «تمرین A · پایین‌تنه»).
- Exercise list: drag handle, name, a chip like «۳ × ۱۲–۱۵». A row opens to: تکرار / ثانیه, sets (− / +), range from–to.
- «حرکت جدید» adds an exercise.

### 11 Today (light)
Same as 01 with light tokens.

## Not designed yet (needed before or during build)

- Onboarding (language → reminder times → phone setup → backup folder → baseline entry with past dates → program import).
- Check-in flow (weight → 5 tape numbers → 3 photos).
- Strength test flow.
- Backup / restore screen.
- Day detail (tap a square on the Dashboard).
- Notification layouts (block reminder, important follow-up with a "Done" action).
- English (LTR) version of the final screens. Only an older Today exists in `screens-detailed.html`.
