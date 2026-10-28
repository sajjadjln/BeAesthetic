# Lantern: Design

_Lantern v1, on the `lantern` branch (2026-10-28). The v0.x design of «پیکر» (the workout program) is on `main`, in this file's older version._

**The reference is the HTML mockups** in `docs/design/`. This file says what they decide and where the code follows them.

| Mockup | What it shows | Decisions |
|---|---|---|
| `splash.html` (https://claude.ai/artifact/UjFq29NLD4zABaBfavF23m) | The splash, "Gather", and the name | D190 |
| `character-pick.html` (https://claude.ai/artifact/6MQKkY1faSmU6NKZ569bTL) | Onboarding: the character, then Today's layout | D186–D188 |
| `today-v5.html` (https://claude.ai/artifact/DUEzm7WHa2QJNDXnQTa2zw) | Today, both layouts, all four themes | D186–D188 |
| `settings-task.html` (https://claude.ai/artifact/DCcA6oTcrkC7VENQVCgpok) | Settings and the new-task sheet | D189 |
| `goal-step1.html` … `goal-step5.html` | The goal flow (after v1) | D185 |

## The name

«فانوس» in Persian, **Lantern** in English (D190). The line under it: «یه چراغ برای هر روز» / "A light for every day".

## The look: a night sky in pixel-game style

- **Background:** always a night sky: a dark color at the edges and a lighter one glowing from the top. No light mode (D189).
- **Pixel shapes:** stepped "pixel" corners instead of round ones. Cards and buttons have one 6 dp step, small chips one 3 dp step, the dialog box two 4 dp steps (`ui/theme/Pixel.kt` → `Px`).
- **The dialog box:** the character speaks in a pixel-game box: a light rim, a dark gap and a deep rim, faint scanlines, the text typed letter by letter, and a small arrow that nudges when the line is done (D185).
- **Font:** Estedad only, bundled.

## The characters

Four characters in one style (D182, D184): the **knight**, the **vampire**, the **wizard** and the **goth girl**. Art in `characters/`: a full figure, a portrait and a sheet of 6 moods each.

- **The user picks one** on first launch and can change it in Settings (D186, D189).
- **The app uses three moods:** calm, busy, happy (`res/drawable-nodpi/char_<character>_<mood>.jpg`, cut by `scripts/cut-character.swift`). Nervous, cross and asleep are never shown in v1.
- **Moods follow the day, never a miss** (D186): calm before the first tick, busy after it, happy when everything is done; at night calm (or happy if the day is complete).
- **The knight's red embers are cyan** (D183): recolored once, offline, by the cut script.
- **Each character has its own voice:** lines per mood in both languages (`lines_<character>_<mood>`), plus its hello in onboarding, Settings and the task sheet.

## Colors

The theme comes from the picked character (D187). Values are in `ui/theme/Colors.kt`.

| Character | Glow (its color) | Sky |
|---|---|---|
| Knight | cyan `#5FD3E6` | night blue `#0B1622` → `#12283A` |
| Vampire | red `#FF5A6A` | near black red `#14070B` → `#2E0F17` |
| Wizard | silver gray `#CFD6DD` | charcoal `#0E1013` → `#23272D` |
| Goth girl | purple `#B98CFF` | violet night `#100A1C` → `#24173C` |

These never change with the character:
- **Done is green** `#3FBF7F`.
- **Due and important are amber** `#F2B84B` («موعدش رسیده» / «مهم»).
- **Special days are indigo.**

**Red never means missed.** The vampire's red is his identity, nothing else. Nothing in the app is ever a "missed" color; a missed day is simply not there.

## Screens

### Splash (D190, D191 ③)
Stars come out; the four portraits drift in from the dark one by one, each in its own glow, and meet in one light at the center. Then «فانوس», "LANTERN", the line and a pixel bar. Full (~4.4 s) until onboarding is done, then about 1.3 s. A tap skips it. Android 12+ shows only the night and a pixel lantern before it. The launcher icon is the same lantern.

### Onboarding (D186, D188)
1. **Character:** the big portrait with its name tag, its hello in the dialog box, the four as pixel cards (the others dimmed). A tap re-themes the app at once; a second tap makes it smile. «بازگردانی از پشتیبان» under the main button.
2. **Today's layout:** two pixel cards with a sketch each; the character explains the picked one.
3. Reminders and the notification permission → phone setup → backup folder → done.

### Today (D186–D188)
Today shows today only: no week row, no history.
- **Header:** the weekday and date in the character's color; the minimum-day and Settings buttons.
- **The character** with its line in the dialog box; the line names the task to do now. Tap for the next line.
- **Progress:** one pixel block per task, green when done, and «۲ از ۷».
- **Layout 1, the list:**
  - The «الان» card: the first open task in the current part, then later parts, then anything still open from earlier. One tap ticks it.
  - Then every part of the day as pixel task rows. The current part's label is in the character's color.
- **Layout 2, the parts slider:** a pixel bar with the four parts and their pixel icons (sunrise, sun, sunset, moon) over swipeable pages.
  - It opens on the clock's part. The indicator follows the finger, and a later part comes in from the left.
  - A green square under a part means it's all done.
- **Minimum day:** only the floor tasks, as one list, in both layouts.
- Under the tasks: «هر وقت», then the reminder-health and backup rows when they have something to say.
- «+» is a pixel button at the bottom corner.

### New task (D128, D189)
A pixel sheet with the character at the top (its hello; it smiles and answers on «ذخیره»).
- **Simple:** the name, typed in the character's color over an underline that lights up; then «کِی؟» and «ذخیره».
- **«تنظیمات بیشتر»** opens the full form:
  - the part of the day;
  - repeat;
  - «بعد از…»;
  - «زنگ», with the day squares only for daily or chosen days;
  - «مهم» and «روز حداقلی».
- Picked choices are in the character's color. No workout switch.

### Settings (D189)
- The reminder-health line.
- **«همراه»:** the character with a line in its voice, and the four pixel cards (a tap re-themes the app and the widget).
- **«صفحه‌ی امروز»:** list or parts.
- **«زبان»:** like the phone, فارسی, English.
- Rows to tasks, reminders, backup and phone setup; the version.
- No «ظاهر» screen and no light mode.

The screens it opens keep their v0.x layout on the night sky, with pixel cards, buttons and calendar.

### Goals (D191)
The character with a «در دست ساخت» tag and one line. The goal flow (`goal-step*.html`) comes after v1.

### Widget
Today's tasks for the current part, one tap each, in the character's colors.

## Language (D174–D176)

English and Persian. Persian is right to left with Persian digits and Jalali dates; English is left to right with Latin digits and Gregorian dates. The language follows the phone unless picked in Settings. The date picker is the Jalali calendar in both (D129).

## Motion

Compose animations only. "Remove animations" in the phone's settings makes all of it instant.
- **The characters:** they breathe, and moods change with a fade.
- **The dialog box:** it types its line letter by letter.
- **Taps:** the progress blocks and done rows fill with a short color change.
- **The splash:** a single timeline.
- **Haptics:** a light tap on a tick, and a medium one when the day is complete.

## Not designed yet

- English screenshots of every screen (checked in L9 on the phone).
- The goal flow's place in v1.x, the goals wall, the dashboard, plugins.
