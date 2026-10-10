# BeAesthetic: Design

**Current design:** `docs/design/screens.html` (interactive; open it in a browser). It holds every designed screen, in Estedad, with an accent bar at the top: pick any of the five colors and every screen follows.
Published copy: https://claude.ai/artifact/BwuSiLCGAxV5fGR86MSmjH

**Fonts and accent colors (the comparison behind D46–D47):** `docs/design/type-and-color.html` (https://claude.ai/artifact/WFBdvYr9iUavDkA1ifyZyE). The six fonts that were compared, Today in all five accents, and the «ظاهر و زبان» screen. Decided in D46–D47: **Estedad**, five accents, **green by default**.

**Name and logo (D77, D78):** the app is «پیکر» in Persian, "BeAesthetic" in English (D79). Logo concepts: `docs/design/logo.html` (https://claude.ai/artifact/2o9iY6KqvcGFTbhYhFF6j9). Direction: a cartoon green sticker person flexing (D80); the exact variant is not picked yet.

**Motion demo (D76):** `docs/design/motion.html` (https://claude.ai/artifact/SdjkRYt5z13an56aZ99huE). Tap through the Today, Workout, Check-in and Dashboard phones; slow motion and "Remove animations" are in the top bar.

Kept for reference only:
- `docs/design/screens-minimal.html`: minimal concept (https://claude.ai/artifact/SuxRScjnPrCJkpGEdogd7z)
- `docs/design/screens-detailed.html`: detailed concept (round 2)
- Figma https://www.figma.com/design/1DzXN6iOhAa0PsnLDSjOAg: first design-system pass. **Out of date** (old names, old Today).

The history of how we got here is in `docs/DECISIONS.md` (D21–D29, D46–D47).

## Visual direction: "quiet strength"

- **Dark first**, with a light theme. The app follows the system theme.
- **One accent, chosen by the user**: green (default), blue, turquoise, violet or orange. The accent always means done or progress, and nothing else uses it.
- **No red anywhere.** Gray = a gap. Amber = due or important, always with words («موعدش رسیده», «مهم»); with the orange accent it becomes yellow. Indigo = special day (sick, travel, off); with blue it becomes violet, with violet it becomes cyan.
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
| accent, accent-subtle, on-subtle, grid level 1…4 | see **Accent palettes** | | Primary buttons, done state, ring, done tiles, positive chips, dashboard squares, week dots |
| grid level 0 | `#262C2A` | `#DCE1DE` | Gap (same in every accent) |
| special / subtle / on-subtle | `#5560C9` / `#1F2448` / `#8C95EA` | `#8C95EA` / `#D3D7F8` / `#3E479E` | Special days, minimum day (blue and violet accents override it) |
| amber / subtle | `#E8B04B` / `#36280D` | `#B07610` / `#FBEFD6` | Due and important (the orange accent overrides it) |

### Accent palettes

Each cell: accent · accent-subtle · on-subtle · grid levels 1–4. Overrides are listed where they apply.

| Accent | Dark | Light |
|---|---|---|
| **سبز Green** (default) | `#34C47A` · `#0F2E1F` · `#5BD994` · `#114029` `#176B43` `#22A565` `#5BD994` | `#1D8A55` · `#E4F7EC` · `#176B43` · `#C4F0D6` `#93E6B6` `#22A565` `#176B43` |
| **آبی Blue** | `#4D9BF0` · `#0F2440` · `#82BBFA` · `#112E52` `#1A4D8A` `#2F7AD6` `#82BBFA`; special `#9A66D4` / `#2B1C42` / `#C6A2F0` | `#1F6BD0` · `#E2EDFC` · `#1A56A8` · `#CDE0FA` `#98C1F3` `#3C86E0` `#1A56A8`; special `#B08BE6` / `#EADCFA` / `#6A3DAE` |
| **فیروزه‌ای Turquoise** | `#2CC2B4` · `#0C2E2B` · `#62DCCF` · `#0E3C38` `#13675F` `#1EA195` `#62DCCF` | `#0E8379` · `#DDF4F1` · `#0A665E` · `#C0ECE7` `#8BDCD3` `#1FA195` `#0A665E` |
| **بنفش Violet** | `#9B7BEA` · `#241B3E` · `#C2AAFA` · `#2B2150` `#46358A` `#7458D0` `#C2AAFA`; special `#3A9FC2` / `#0E2A33` / `#7CCBE4` | `#6644C8` · `#EDE7FC` · `#5233AA` · `#DCD1FA` `#B8A4F2` `#8466DE` `#5233AA`; special `#6CC0DC` / `#D4EEF6` / `#1D6A84` |
| **نارنجی Orange** | `#F28B3C` · `#3A220F` · `#FFB27A` · `#3D2410` `#6E3D16` `#C2661F` `#FFB27A`; due/important `#E3D04E` / `#302C0B` | `#C2570F` · `#FCEADB` · `#9A440C` · `#FBD9BE` `#F6B485` `#E07B2E` `#9A440C`; due/important `#776804` / `#F6F0C6` |

Contrast was checked: text on accent buttons ≥ 4.3:1 (bold 16 px), accent chip text ≥ 4.8:1, in every accent and mode.

- **Spacing:** 4 · 8 · 12 · 16 · 24 · 32.
- **Radius:** 6 (grid cells) · 12 (controls) · 16 (cards) · 20 (tiles) · 28 (sheets) · full.
- **Font:** **Estedad** (Amin Abedi, SIL OFL), bundled in the APK; also used for the English UI. Persian digits in Persian UI. (Vazirmatn was the font until D46.)

| Style | Size / weight |
|---|---|
| Display number | 52–96 / ExtraBold (ring center, workout counter) |
| Headline | 24–26 / ExtraBold |
| Title | 18 / Bold |
| Subtitle | 16 / SemiBold |
| Body | 15 / Regular–Medium |
| Label | 13 / Medium |
| Caption | 12 / Regular |

## Motion (D76)

**Calm by default, generous at the moments that matter.** Motion only confirms something the user did or shows progress. Demo: `docs/design/motion.html` (https://claude.ai/artifact/SdjkRYt5z13an56aZ99huE).

Rules:
- Motion follows action: no idle loops, nothing pulses for attention.
- Only the accent moves. Gray days, misses and «موعدش رسیده» never animate.
- Things that fill over time (grid, charts, program line) fill right → left in Persian, mirrored in English.
- Every satisfying moment has a vibration. No sound.
- "Remove animations" on the phone → every duration is zero; the end state appears at once.

| Token | Value | Used for |
|---|---|---|
| tap | 100 ms | Press feedback, control colors |
| short | 200 ms | Tile fill, check mark, chips, fades |
| medium | 350 ms | Ring, part-of-day indicator, tiles settling, sheets |
| long | 600 ms | Odometer, reveal wipe, chapter line, 100% shimmer |
| story | 2–4 s, skippable | Halfway and finish recaps only |
| standard easing | `cubic-bezier(.2, 0, 0, 1)` | Most motion |
| spring | damping ≈ 0.7 | Ring arc, press release, sheets |
| settle | damping ≈ 0.85 | Layout changes: indicator, tiles moving, chapter marker |
| bouncy | damping ≈ 0.5 | Only «رکورد», round numbers, part-of-day dots |

| Vibration | When |
|---|---|
| light (`CONFIRM`, fallback `VIRTUAL_KEY`) | Tick, set logged, part of the day finished, chapter start |
| medium (`EFFECT_HEAVY_CLICK`) | 100% day, round number crossed |
| record (waveform 0, 30, 60, 30 ms) | «رکورد» only |
| none | Untick, navigation, anything about a missed day |

Where motion is used:

| Tier | Where | What |
|---|---|---|
| Everyday | Tile tick | Accent fill spreads from the touch point, check draws itself, tile dips to 96% and springs back, light vibration. Untick drains back, no vibration. |
| Everyday | Day ring | Arc springs to its new value; the center number rolls. |
| Everyday | Part-of-day switcher | Indicator slides; tiles of a later part enter from the left (time flows right → left). |
| Everyday | Part of the day finished | Its dot pops; the ring glows once. |
| Everyday | Minimum day | Leaf turns; non-floor tiles fade and fold; the rest settle into place. |
| Everyday | Week dots | Today's dot fills to its new level. |
| Everyday | Workout tile → session | The tile expands into the session screen (shared element). |
| Everyday | Sheets, tabs | Standard spring sheets, fade-through between tabs. |
| Moment | 100% day | Ring closes, a shimmer runs around it once, number grows, «امروز کامل شد» fades in, medium vibration. Once a day. |
| Moment | Set logged | The number flies into its set circle; the next set comes into focus. |
| Moment | «رکورد» | Burst on the set circle, the chip springs in, record vibration. |
| Moment | Next rung | The next ladder step lights up and lifts. |
| Moment | Round number | Odometer rolls past it, a halo once, medium vibration. |
| Moment | Comeback | «خوش برگشتی» fades in and stays still. |
| Moment | Strength-test guess | Guess and result bars grow; «از حدست بهتر» when beaten. |
| Moment | Dashboard, first open of the day | Squares appear week by week, right → left, ~0.4 s; today glows once. |
| Moment | Charts | The line draws in the direction of time; the record point last. |
| Milestone | Halfway recap (Yalda) | Story cards: the grid fills to today, numbers roll, Day 0 vs now photos. |
| Milestone | Finish (29 Esfand) | Whole grid fills, program line reaches «نوروز ۱۴۰۶», a sprout grows in the accent. No confetti. |
| Milestone | Check-in reveal | Day 0 appears, then a wipe reveals today's photo on the left; the slider stays. |
| Milestone | New chapter | Program-line marker moves to the next chapter; the chapter card slides up. |
| Milestone | Letter opening | The sealed envelope opens; the text unfolds. |

Not used: confetti on ordinary taps, a mascot, idle loops, shake or flash on errors, any motion on missed days, sound.

## Navigation

- Bottom bar with **icons only**, 4 tabs, RTL order from the right: **Today** (check-circle) · **Dashboard** (grid) · **Progress** (trend line) · **Week** (calendar). The active tab has a small green dot.
- **Settings** opens from the gear on Today.
- Full-screen flows: Workout session, Check-in, Friday ritual, Onboarding, Reliability setup, Backup/restore.

## Screens (current)

### 01 امروز (Today)
- Header: weekday (large), "۱۸ آبان · هفته ۵ از ۲۳ · فصل آبان" (D51); in the last chapter the chapter part becomes «۲۹ روز تا نوروز» (D74); a **leaf** button (minimum day) and a **gear** (settings).
- **Ring**: tasks done today / total (e.g. ۳ از ۱۲). Under it, **7 week dots** (Saturday on the right), colored by the day level; today is outlined.
- **Part-of-day switcher**: 4 icons (sunrise · sun · sunset · moon). A tiny accent dot under an icon means that part is complete.
- **Tiles** (2 columns): an icon + one or two words. Tap toggles done (accent fill + check), with a soft haptic and a ~200 ms fill (D58). A finished part of the day and a 100% day get a slightly bigger moment.
  - Important task: **gold outline** and the word «مهم».
  - Due interval task: **amber outline** and a chip «موعدش رسیده».
  - Workout: a wide tile with a play button that opens the Workout session.
- **Comeback** (D53): on the first tick after a day at level 0, one line under the ring: «خوش برگشتی». Shown once.
- **Cards** (one at a time, above the tiles): chapter start with the letter offer (D51, D72), a letter on its opening day (D72), a round number crossed (D71), recap at Yalda and 29 Esfand (D52), the one-time offer to silence a routine's reminders (D57), plus the existing ritual, check-in and welcome-back cards.
- **Minimum day** (leaf on): the part-of-day switcher hides, «روز حداقلی» shows, and only the floor tiles appear.

### 02 داشبورد (Dashboard)
- Title and the program range.
- Summary: «روز حضور» (e.g. ۳۰ از ۳۱) and «روز ویژه». **No weekly average.**
- «کل مسیر»: a 23-column × 7-row grid of small squares (week 1 on the right, Saturday on top); Jalali month labels below.
- Legend: less → more, special, and «قبل از اپ» (D56): a dotted outline with no fill, for days before the app was installed.
- «هفته به هفته»: one row per week (newest on top): label + date range, 7 squares, and a score chip («جاری» for the current week).

### 03 پیشرفت (Progress)
- Monthly check-in card: «چک‌این ماهانه: جمعه ۱۴ آذر · وزن، متر، ۳ عکس» and the days left.
- 2×2 stat cards since Day 0: pull-ups, push-ups, plank, waist (value, delta chip, starting value).
- Pull-up chart (time right → left; dashed segment = the next test) with a «رکورد» chip.
- Weight card with a sparkline.
- «عکس‌های ماهانه»: one row per month (front / side / back), "2 of 7".
- «نردبان‌ها» (D70): one row per ladder, the rungs as small steps (reached = accent, current = outlined, next = text-3), the current variant's name.
- «از روز صفر» totals (D71): big numbers that only go up (e.g. «۳٬۲۴۰ شنا», «۲۷ تمرین», «۴۱ روز حضور»).
- Program line (D51): a thin line from Day 0 to «نوروز ۱۴۰۶» with the five chapters marked and today's position.
- Milestones (D65): 5–8 rows; reached ones in the accent with the date, others in text-3. No badges.

### 04 تمرین (Workout session)
- Close button, exercise-progress dashes (e.g. 2 of 6).
- Exercise name; set circles (done sets show reps; the current set follows the counter).
- **Huge number** with − / +, pre-filled from last time. Under it: «قبلی ۱۲ · ۱۲ · ۱۰».
- **Seek bar 1–10** with the label «درجه سختی» and the value.
- A big round ✓ button logs the set (ثبت).
- **End of session:** a short summary; a round number crossed shows here («۱٬۰۰۰ شنا از روز صفر») (D71). When every set hit the top of its range: «پله بعد: شنای الماسی؟» with «بله» / «بعداً» (D70).
- **Record** (D54): when a logged set beats that exercise's best, its circle fills with the accent and shows «رکورد» for a moment.

### 05 هفته (Friday review and plan)
- Steps: مرور (done) → برنامه.
- Review card: the week's 7 squares, score chip, wins (records, best routines, comebacks «۲ بار برگشتی»). Wins only, no misses. The identity sentence sits under the card (D65).
- **Short path** (D62): a «شروع هفته ۵» button right under the review; the plan steps below are optional.
- «روزهای ویژه»: 7 day chips; tap to mark special. The label reads «۲ روز ذخیره» (D65); a third tap is refused.
- «میان‌وعده‌ها»: one row per day; closed rows show only the day and a dot (filled = has a note). A row opens to a free-text box (can hold a whole recipe).
- Button «شروع هفته ۵». Finishing runs the weekly backup.

### 06 تنظیمات (Settings)
- «یادآورها سالم‌اند» health banner.
- Six tiles: کارها · یادآورها · برنامه تمرین · پشتیبان · ظاهر و زبان · گوشی (Xiaomi battery/autostart setup).

### 06b ظاهر و زبان (Appearance and language)
- «رنگ»: five named color circles (سبز · آبی · فیروزه‌ای · بنفش · نارنجی); the chosen one has a ring and a check.
- A preview card: a small ring, a slice of the dashboard, and the chips (رکورد, مهم, روز ویژه) in the chosen color.
- «حالت»: سیستم / تیره / روشن.
- «زبان»: فارسی / English.
- «ذخیره».

### 07 کارها (Tasks)
- Filter: همه / صبح / ظهر / عصر / شب.
- Sections: one-off tasks, then each part of the day. Rows show the name and the repeat rule; markers: «مهم» chip, leaf (in minimum day), bell (own alarm), repeat icon (every N days).
- Floating «کار جدید» button.

### 08 کار جدید / ویرایش (Task editor, bottom sheet)
- Name, icon picker (6 icons), part of day (4 icons), repeat (هر روز / روزهای خاص / هر N روز / یک بار).
- «بعد از…» (D55): an optional one-line anchor, e.g. «بعد از صبحانه».
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
- Strength test: an optional «حدس بزن» number before the test; afterwards «حدس: ۸ · واقعی: ۱۰», plus «از حدست بهتر» when beaten, neutral otherwise (D73).
- Check-in reveal (D75): after the 3rd photo, the new photos next to Day 0 with a compare slider; a toggle for last month.
- Letters (D72): a plain text sheet to write; a sealed-envelope card until its day.
- Added by the research decisions (D51–D65), designed in the slice that needs them: chapter card, halfway and finish recaps, the after-the-finish choice (ادامه / برنامه تازه), the ghost-overlay camera, the silence-reminders offer, milestones setup, identity sentence. The changes above to Today, Dashboard, Progress, Workout, Week and the task editor are not in `screens.html` yet.
