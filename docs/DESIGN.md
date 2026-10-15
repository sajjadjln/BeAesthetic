# BeAesthetic: Design

**Current design:** `docs/design/screens.html` (interactive; open it in a browser). It holds every designed screen, in Estedad, with an accent bar at the top: pick any of the five colors and every screen follows.
Published copy: https://claude.ai/artifact/BwuSiLCGAxV5fGR86MSmjH

**Fonts and accent colors (the comparison behind D46–D47):** `docs/design/type-and-color.html` (https://claude.ai/artifact/WFBdvYr9iUavDkA1ifyZyE). The six fonts that were compared, Today in all five accents, and the «ظاهر و زبان» screen. Decided in D46–D47: **Estedad**, five accents, **green by default**.

**Name and logo (D77, D78):** the app is «پیکر» in Persian, "BeAesthetic" in English (D79). Logo concepts: `docs/design/logo.html` (https://claude.ai/artifact/2o9iY6KqvcGFTbhYhFF6j9). **Chosen: the cartoon sticker** (D81): a chunky green bodybuilder flexing, ink outline, white sticker border, slight tilt, on the dark background. Files in `docs/design/logo/`.

**Character «پیکر» (D82–D84):** `docs/design/character.html` (https://claude.ai/artifact/Jf7hbWjUQTf39HRDcVkExZ). The logo's sticker guy as a person in the app: poses, Today and moment screens, chapter outfits, sample lines and rules. The rig that draws him is `docs/design/piker.js`; `screens.html` uses it too.

**Flows (D85):** `docs/design/flows.html` (https://claude.ai/artifact/An8XmUuGo4cWrbHHhWMkom): onboarding, notifications, check-in, strength test, day detail, backup, letters, recaps and the finish, offers, Progress additions. See Flows below.

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
| bg | `#1B201E` | `#F6F7F6` | Screen background |
| surface | `#252B29` | `#FFFFFF` | Cards, tiles |
| surface-2 | `#2F3633` | `#EDF0EE` | Inputs, secondary controls |
| border | `#3C4441` | `#DCE1DE` | Dividers, outlines |
| text-1 / text-2 / text-3 | `#EEF1EF` / `#A1ABA7` / `#828C88` | `#0E1110` / `#5F6965` / `#8A948F` | Text levels |
| accent, accent-subtle, on-subtle, grid level 1…4 | see **Accent palettes** | | Primary buttons, done state, ring, done tiles, positive chips, dashboard squares, week dots |
| grid level 0 | `#3C4441` | `#DCE1DE` | Gap (same in every accent) |
| special / subtle / on-subtle | `#5560C9` / `#313953` / `#A3AAF2` | `#8C95EA` / `#D3D7F8` / `#3E479E` | Special days, minimum day (blue and violet accents override it) |
| amber / subtle | `#E8B04B` / `#4C4630` | `#B07610` / `#FBEFD6` | Due and important (the orange accent overrides it) |

### Accent palettes

Each cell: accent · accent-subtle · on-subtle · grid levels 1–4. Overrides are listed where they apply.

| Accent | Dark | Light |
|---|---|---|
| **سبز Green** (default) | `#34C47A` · `#29503C` · `#5BD994` · `#23513A` `#28754E` `#2FA066` `#5BD994` | `#1D8A55` · `#E4F7EC` · `#176B43` · `#C4F0D6` `#93E6B6` `#22A565` `#176B43` |
| **آبی Blue** | `#4D9BF0` · `#2F4659` · `#82BBFA` · `#2A455D` `#35608B` `#4280C2` `#82BBFA`; special `#9A66D4` / `#433A55` / `#C6A2F0` | `#1F6BD0` · `#E2EDFC` · `#1A56A8` · `#CDE0FA` `#98C1F3` `#3C86E0` `#1A56A8`; special `#B08BE6` / `#EADCFA` / `#6A3DAE` |
| **فیروزه‌ای Turquoise** | `#2CC2B4` · `#274F4A` · `#62DCCF` · `#20514B` `#24746C` `#289E93` `#62DCCF` | `#0E8379` · `#DDF4F1` · `#0A665E` · `#C0ECE7` `#8BDCD3` `#1FA195` `#0A665E` |
| **بنفش Violet** | `#9B7BEA` · `#413E57` · `#C2AAFA` · `#413B5B` `#5E4F88` `#7F67BD` `#C2AAFA`; special `#3A9FC2` / `#2A4951` / `#7CCBE4` | `#6644C8` · `#EDE7FC` · `#5233AA` · `#DCD1FA` `#B8A4F2` `#8466DE` `#5233AA`; special `#6CC0DC` / `#D4EEF6` / `#1D6A84` |
| **نارنجی Orange** | `#F28B3C` · `#56422E` · `#FFB27A` · `#5C4027` `#8B582E` `#C37335` `#FFB27A`; due/important `#E3D04E` / `#4B4C30` | `#C2570F` · `#FCEADB` · `#9A440C` · `#FBD9BE` `#F6B485` `#E07B2E` `#9A440C`; due/important `#776804` / `#F6F0C6` |

Contrast was checked: text on accent buttons ≥ 4.3:1 (bold 16 px), accent chip text ≥ 4.8:1, in every accent and mode.

The dark background is "slate" (D136), lifted from near-black so cards, borders and empty squares read. In dark mode the subtle fills and grid levels 1–3 are mixed from the accent: subtle = 24 % accent over surface (special 26 %, amber 20 %), grid 1–3 = 30 / 52 / 78 % accent over bg. Options compared: `docs/design/dark-bg-options.html`.

- **Spacing:** 4 · 8 · 12 · 16 · 24 · 32.
- **Radius:** 6 (grid cells) · 12 (controls) · 16 (cards) · 20 (tiles) · 28 (sheets) · full.
- **Font:** **Estedad** (Amin Abedi, SIL OFL), bundled in the APK. Persian digits everywhere (D87). (Vazirmatn was the font until D46.)

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
- Things that fill over time (grid, charts, program line) fill right → left.
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

Not used: confetti on ordinary taps, idle loops (the character included), shake or flash on errors, any motion on missed days, sound.

«پیکر» (D82) moves only when his pose changes: scale 0.88 → 1 with the bouncy spring (~340 ms), and his bubble fades in (short). On "Remove animations" both are instant. He adds no vibration of his own.

## The character «پیکر» (D82–D84)

Board: `docs/design/character.html`. Rig: `docs/design/piker.js` (17 poses, faces, props, outfits).

- **Who:** the logo's sticker guy as a hype gym buddy. Warm, a bit goofy, proud of you. Same construction as the logo: ink outline, white sticker border, soft shadow, slight tilt. In one-arm poses the free arm hangs flared out from the shoulder (a bodybuilder's relaxed lat stance), with a gap from the torso. Body = the accent color; ink, paper and props are fixed. The launcher icon stays green.
- **Voice:** Persian, first person, informal, one short line (about 8 words max). Real numbers only («رکورد! ۸ تا بارفیکس»). Hypes actions, never the body, weight or looks. Never sad or disappointed at you; never names or hints at a missed day. Lines live in string resources, several per moment, and don't repeat within 7 days.
- **Today:** one row under the ring and week dots: «پیکر» (~64 dp, start side) and a bubble (surface-2, 16 dp radius, a tail pointing at him). The line follows the day: morning (stretch) → after a tick (thumbs up) → a part done (cheer) → 100% (flex) → night (sleep); minimum day and special days (tea); comeback (welcome, «خوش برگشتی»). Tap him: a new line and a flex or a wink.
- **Big moments** (centered, ~170 dp, bubble under him): 100% day, «رکورد» in the workout, chapter-start card (letter + the new outfit), check-in day (selfie) and reveal (wow), Friday wins (cheer), round numbers (cheer), Yalda (Hafez book), Esfand (sabzeh), finish (flex + goldfish), empty screens (think) and onboarding (wave).
- **Outfits (D83):** ① none · ② sweatband · Yalda Hafez book · ③ beanie and scarf · ④ sunglasses · ⑤ sprout and sabzeh · finish goldfish. From the calendar only.
- **Not:** in notifications, a pet, idle-animated, or shown on a screen about a missed day.
- **Off switch:** «ظاهر» → «پیکر در صفحه‌ی امروز» (on by default). Off: the Today row hides and moments fall back to their plain text.

## Navigation

- Bottom bar with **icons only**, 4 tabs, RTL order from the right: **Today** (check-circle) · **Dashboard** (grid) · **Progress** (trend line) · **Week** (calendar). The active tab has a small green dot.
- **Settings** opens from the gear on Today.
- Full-screen flows: Workout session, Check-in, Friday ritual, Onboarding, Reliability setup, Backup/restore.

## Screens (current)

### 00 شروع (Splash)
- Dark background; the app icon (cartoon sticker, D81) in Android's 160 dp circle; «پیکر» with "BeAesthetic" under it at the bottom. The icon stays green whatever the accent: it is the launcher icon.

### 01 امروز (Today)
- Header: weekday (large), "۱۸ آبان · هفته ۵ از ۲۳ · فصل آبان" (D51); in the last chapter the chapter part becomes «۲۹ روز تا نوروز» (D74); a **leaf** button (minimum day) and a **gear** (settings).
- **Ring**: tasks done today / total (e.g. ۳ از ۱۲). Under it, **7 week dots** (Saturday on the right), colored by the day level; today is outlined.
- **Part-of-day switcher**: 4 icons (sunrise · sun · sunset · moon). A tiny accent dot under an icon means that part is complete.
- **Tiles** (2 columns): an icon + one or two words. Tap toggles done (accent fill + check), with a soft haptic and a ~200 ms fill (D58). A finished part of the day and a 100% day get a slightly bigger moment.
  - Important task: **gold outline** and the word «مهم».
  - Due interval task: **amber outline** and a chip «موعدش رسیده».
- **«تمرین امروز» card** (D126): between the «پیکر» row and the part switcher, so it shows in every part. Workout name, moves and sets, a tick circle and ▶. Card or ▶ opens the session; the circle ticks without logging. The linked task doesn't appear as a tile. On a day without a plan: one quiet row, «امروز استراحت. فردا: …», that opens the plan.
- **«هر وقت» card** (D127): folded at the bottom, «هر وقت · N». Opens with one tap; items have a round check; a ticked item fades out. «یه چیز اضافه کن» opens the simple sheet on «هر وقت». Hidden when empty. Never counts in the ring.
- **«پیکر» row** (D82): under the ring and week dots, the character and one line that follows the day (see The character). He also says the **comeback** line (D53): on the first tick after a day at level 0, «خوش برگشتی», shown once, with the welcome pose.
- **Cards** (one at a time, above the tiles): chapter start with the letter offer (D51, D72), a letter on its opening day (D72), a round number crossed (D71), recap at Yalda and 29 Esfand (D52), the one-time offer to silence a routine's reminders (D57), plus the existing ritual, check-in and welcome-back cards.
- **Minimum day** (leaf on): the part-of-day switcher hides, «روز حداقلی» shows, and only the floor tiles appear.

### 02 داشبورد (Dashboard)
- Title and the program range.
- Summary: «روز حضور» (e.g. ۳۰ از ۳۱) and «روز ویژه». **No weekly average.**
- «کل مسیر»: a 23-column × 7-row grid of small squares (week 1 on the right, Saturday on top); Jalali month labels below.
- Legend: less → more, special, and «قبل از اپ» (D56): a dotted outline with no fill, for days before the app was installed.
- «هفته به هفته»: one row per week (newest on top): label + date range, 7 squares, and a score chip («جاری» for the current week).
- As built (D123–D125): the grid includes Day 0's and the finish day's weeks (25 columns); the week row of Day 0 is «روز صفر». Week chips: accent from 80 %, neutral below. The tab bar has Today and Dashboard only for now. The day sheet (D1) shows the Gregorian date in Persian («۱۱ اکتبر ۲۰۲۶»), the level line, «انجام شد» chips (and «رکورد …» for records that day), «روز ویژه بود؟» and, for yesterday, the tick chips.

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
- As built (D115–D118): the dashes jump between moves; «پایان» (top, once a set is logged) shows the summary; the line under the name holds the plan («۳ × ۸–۱۲، ۶ کیلو، هر طرف…») and the note; a unit line («تکرار، هر طرف») under the number; the kg chip under it; a done circle can be tapped to correct the set; the summary has «پیکر» cheering, the set and record counts, each move's values, «پایان» and «ادامه‌ی تمرین». A day without a plan shows «پیکر» thinking and a button to the plan editor.

### 05 هفته (Friday review and plan)
- Steps: مرور (done) → برنامه.
- Review card: the week's 7 squares, score chip, wins (records, best routines, comebacks «۲ بار برگشتی»). Wins only, no misses. «پیکر» cheers one line about the best win at the bottom of the card (D82). The identity sentence sits under the card (D65).
- **Short path** (D62): a «شروع هفته ۵» button right under the review; the plan steps below are optional.
- «روزهای ویژه»: 7 day chips; tap to mark special. The label reads «۲ روز ذخیره» (D65); a third tap is refused.
- «میان‌وعده‌ها»: one row per day; closed rows show only the day and a dot (filled = has a note). A row opens to a free-text box (can hold a whole recipe).
- Button «شروع هفته ۵». Finishing runs the weekly backup.

### 06 تنظیمات (Settings)
- «یادآورها سالم‌اند» health banner.
- Six tiles: کارها · یادآورها · برنامه تمرین · پشتیبان · ظاهر · گوشی (Xiaomi battery/autostart setup).
- At the bottom: the app icon, «پیکر» and «نسخه ۰.۱ · BeAesthetic».

### 06b ظاهر (Appearance)
- «رنگ»: five named color circles (سبز · آبی · فیروزه‌ای · بنفش · نارنجی); the chosen one has a ring and a check.
- A preview card: a small ring, a slice of the dashboard, and the chips (رکورد, مهم, روز ویژه) in the chosen color.
- «حالت»: سیستم / تیره / روشن.
- «پیکر در صفحه‌ی امروز»: a switch, on by default (D84).
- «ذخیره».

### 07 کارها (Tasks)
- Filter: همه / صبح / ظهر / عصر / شب.
- Sections: «هر وقت» (D127), then dated one-off tasks, then each part of the day. A part filter hides «هر وقت». The workout task has a dumbbell marker (D126).
- Rows show the name and the repeat rule; markers: «مهم» chip, leaf (in minimum day), bell (own alarm), repeat icon (every N days).
- Floating «کار جدید» button.

### 08 کار جدید / ویرایش (Task editor, bottom sheet)
- Name, icon picker (6 icons), part of day (4 icons), repeat (هر روز / روزهای خاص / هر N روز / یک بار).
- «بعد از…» (D55): an optional one-line anchor, e.g. «بعد از صبحانه».
- **«زنگ»** card with a switch: the time (large) and **7 day circles** to choose which weekdays the alarm rings.
- Toggles: «مهم» (one extra reminder), «روز حداقلی» (part of the floor).
- «ذخیره».
- **Simple first (D128):** the sheet opens with the name and «کِی؟» (هر وقت · امروز · فردا · یه روز… · هر روز), a line saying which day and part, «تنظیمات بیشتر» and «ذخیره». The full form below is behind «تنظیمات بیشتر». Mockup: `tasks-v2.html` C–E.
- In the full form: «هر وقت» joins the repeat choices and hides part, anchor, alarm, floor and «کار تمرین». «یک بار» is one date row that opens the **Jalali calendar** (D129). The alarm shows the day circles only for «هر روز» and «روزهای خاص»; otherwise it's a time with «همون روز» or «روزهایی که موعدشه» (D130). The «کار تمرین» switch (D126) sits at the end.
- As built (D107): all 11 icons; a 5th repeat choice «فقط روز حداقلی»; «هر N روز» as a − / + stepper; «یک بار» as day pills for the next 4 weeks (now the calendar, D129); suggestion chips under «بعد از…»; «حذف» under «ذخیره» for an existing task. Problems show in amber after the first «ذخیره» (D110).

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
- As built (D105, D114, D118): the day pills show the number of moves (or «—»), not a letter. The name saves as you type. Rows show the drag handle, the name, a details line (kg, per side, effort, rest) and the sets chip. A row opens a bottom sheet (not inline): name with suggestions of moves from other days, تکرار / ثانیه, sets, «حداکثر» or from–to, «وزنه» with kg, «هر طرف», target effort, rest, note, «ذخیره», «برداشتن از این روز».
- Today's workout tile (D117): wide, first in its part, a check circle and the accent play button.
- «حرکت سخت‌تر» (D119): a field in the move sheet with suggestions (never the move itself or an easier one); a loop shows an amber line.
- Slice 4b in the session: the ladder line under the name («پله‌ی ۱ از ۲، بعدی: …» or «آخرین پله»). The summary shows round numbers as accent pills that land with a bounce (D121) and offer cards (D120) in the accent-subtle color with «بله» / «بعداً»; «بله» turns into «برنامه عوض شد». Correcting a set stays on that set; «پایان» opens the summary.
- Today (D121): a days-shown-up card with «پیکر» cheering and «دمت گرم».

### 11 Today (light)
Same as 01 with light tokens.

## Flows (`docs/design/flows.html`, D85)

Published: https://claude.ai/artifact/An8XmUuGo4cWrbHHhWMkom. Every screen that wasn't in `screens.html`, same tokens and character.

- **Onboarding (O1–O8):** سلام → مسیر (Day 0 and finish dates, the 5 chapters) → یادآورها (block times, normal/Friday, bedtime) → گوشی (5 reliability rows + test notification) → پشتیبان (folder, auto backup) → روز صفر (weight, 5 tape sites, strength tests, 3 photos from the gallery, optional workout backfill) → برنامه (paste the plan, preview what was understood) → جمله و هدف‌ها (identity sentence, 5–8 milestone chips). A dot row shows the step.
- **Notifications (N1):** block reminder (count, first names, anchors), the important follow-up with «انجام شد», a task's own alarm, the check-in morning. Neutral words; the white sticker silhouette as the icon; no character.
- **گوشی in Settings (N2):** this week's reminder health (one bar per reminder; late ones shorter and neutral), the same 5-row checklist, the test button, and the quiet «یه یادآور دیر رسید · درستش کن» banner Today shows.
- **Check-in (C1–C5):** the indigo check-in card on Today with «پیکر» (selfie) → weight stepper (±0.1, pre-filled) → 5 tape sites with where to measure, pre-filled → camera with last month's ghost outline, a 10 s timer and the consistency checklist → reveal with a drag divider (Day 0 / last month, front / side / back), deltas in the accent, weight neutral.
- **Strength test (S1–S4):** the session list (5 tests, current one highlighted) → optional guess (pre-filled) → a big timer for timed tests where one lap of the ring = the last result → guess vs result bars; beating the guess adds «از حدست بهتر» and «پیکر» goes wow, a lower result is just two neutral numbers.
- **Day detail (D1):** a bottom sheet over the dashboard: date (Jalali, Gregorian), the level as words and a number, done items only as accent chips, «روز ویژه بود؟» and «ویرایش تیک‌های دیروز» for today and yesterday; older days read-only.
- **Backup (B1–B2):** status card first, «همین الان پشتیبان بگیر», folder and auto switch, the last 8 backups, «بازگردانی از فایل». Restore is a preview sheet (date, version, counts) with the real safety behavior in words; amber only when the last backup is over 10 days old.
- **Chapters and letters (L1–L3):** the chapter-start card replaces the «پیکر» row for the day (new outfit, chapter line, letter offer); the letter sheet with its opening date and the seal rule; on its day an envelope card that opens on tap, with today's real numbers under the letter.
- **Recaps and the finish (R1–R3, F1–F2):** story cards with a progress bar on top: the grid filling with days shown up, Day 0 → now numbers with lifetime totals and milestones, Day 0 and latest photo with the identity sentence; the finish card with the full program line and the sprout; the after-the-finish choice (ادامه / برنامه تازه).
- **Offers and Progress (K1, K2, P1):** the one-time silence-reminders offer framed as a win; the milestones list (reached in the accent with the date, open ones with plain progress) and the new-milestone form; Progress additions: program line with chapter ticks, ladders, lifetime totals, sealed letters, milestones.

`screens.html` also got the research changes it was missing: the chapter line on Today, «قبل از اپ» cells on the Dashboard, the ladder line in the Workout, comebacks in the Friday wins, the «بعد از…» anchor in the task editor.

## Language (D87)

Persian only, RTL, Persian digits. There is no English UI and no language setting. An English version, if it ever comes, is a separate build with its own strings and an LTR layout.

## Not designed yet

- The halfway recap's 4th card («نیمه‌ی دوم») and the finish recap's middle cards reuse R1–R3 with all 23 weeks.
