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
- **Font:** Estedad only, bundled, in every look.

## Two looks: with a companion, or Simple (D200)

- **With a companion** (the default): everything above and below, in the pixel style.
- **Simple:** no character anywhere and flat, rounded shapes: 14 dp cards, 10 dp chips, a 22 dp sheet top (`Px` returns rounded shapes when `LocalCompanion` is false). The check is a rounded square with a plain stroked tick; progress is thin rounded segments; no dashed lines under the parts. The color is one of the same four palettes, picked by color (فیروزه‌ای، قرمز، نقره‌ای، بنفش) and stored as the character, so switching back keeps the colors.
- Stored in `settings.showBuddy` (true = companion). Picked in onboarding step ① and in Settings → «ظاهر». Mockup: `docs/design/simple-mode.html`.

## The bar and «+» (D201)

- The bottom bar is Today · **+** · Goals. «+» sits in the middle and rises 20 dp above the bar. It opens the new-task sheet on Today from either tab.
- **«+» is the one thing each character changes.** Everything else is the same design in every character:
  - **Knight:** a pixel key on its shadow; pressed, it sinks 3 dp.
  - **Vampire:** a coffin in his red with a carved inner rim, on its shadow; pressed, it sinks.
  - **Wizard:** a glowing crystal orb with a soft halo and a small twinkling star.
  - **Goth girl:** a moon with a bite out of its top corner and two small stars.
  - **Simple:** a rounded square.
- Code: `ui/components/BottomBar.kt`. Mockup: `docs/design/character-styles.html`.

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
**Current (D197):** the user's gold logo on the night sky. The lantern shows alone and dim, its flame catches, then its light writes the name in reading order (Persian right to left, English "Lantern" left to right with the lantern on the «L»). No text below. Previews: `splash-logo.html`, `splash-logo-en.html`. The firefly and Gather notes below are history.
**Selected revision (D193):** Follow the firefly (option 6), with the wordmark and tagline at the top and the new cyan launcher lantern with its amber flame below. Revised preview: `docs/design/splash-options.html`. The implementation described below still uses Gather.

Stars come out; the four portraits drift in from the dark one by one, each in its own glow, and meet in one light at the center. Then «فانوس», "LANTERN", the line and a pixel bar. Played in full on every cold start, with a short hold on the name at the end (D194). A tap skips it. Android 12+ shows only the night and a pixel lantern before it. The launcher icon uses a wider pixel lantern with cyan metal and a warm amber flame, a continuous night-sky glow, and a matching monochrome silhouette. The system splash keeps its original cyan lantern. Preview: `docs/design/launcher-icon-preview.png` (D192).

### Onboarding (D186, D188, D200)
1. **Look:** «با همراه» or «ساده», two cards with a line each. «بازگردانی از پشتیبان» under the main button.
2. **Character** (with a companion): the big portrait with its name tag, its hello in the dialog box, the four as pixel cards (the others dimmed). A tap re-themes the app at once; a second tap makes it smile. In Simple: **color**, the four palettes by color.
3. **Today's layout:** two cards with a sketch each; the character explains the picked one (in Simple each card's own line does).
4. Reminders and the notification permission → phone setup → backup folder → done.

### Today (D186–D188)
Today shows today only: no week row, no history.
- **Header:** the weekday and date in the character's color; the minimum-day and Settings buttons.
- **The character** with its line in the dialog box; the line names the task to do now. Tap for the next line. In Simple, a card with the count «۳ از ۹» and one quiet line instead.
- **Progress:** one pixel block per task, green when done, and «۲ از ۷».
- **Layout 1, the list:**
  - The «الان» card: the first open task in the current part, then later parts, then anything still open from earlier. One tap ticks it.
  - Then every part of the day as pixel task rows. The current part's label is in the character's color.
- **Layout 2, the parts slider:** a pixel bar with the four parts and their pixel icons (sunrise, sun, sunset, moon) over swipeable pages.
  - It opens on the clock's part. The indicator follows the finger, and a later part comes in from the left.
  - A green square under a part means it's all done.
- **Minimum day:** only the floor tasks, as one list, in both layouts.
- Under the tasks: «هر وقت», then the reminder-health and backup rows when they have something to say.
- «+» is in the middle of the bar (D201).

### New task (D128, D189, D201)
The flat layout from `simple-mode.html`, in every look. At the top, the title «کار جدید» (or «ویرایش کار») with the character's small portrait and its hello beside it; it smiles and answers on «ذخیره». In Simple, only the title and a plain line.
- **Simple form:** «اسم کار» over a filled field (its edge in the character's color while typing, amber when a save needs a name); «کِی؟» as chips with the help line; «بخش روز» as four chips (not for «هر وقت»); then «تنظیمات بیشتر».
- **«تنظیمات بیشتر»** opens the full form:
  - the part of the day;
  - repeat;
  - «بعد از…»;
  - «زنگ», with the day squares only for daily or chosen days;
  - «مهم» and «روز حداقلی».
- Picked choices are in the character's color. No workout switch.
- **«ذخیره» is pinned** at the bottom of the sheet and always shows; everything above it scrolls (D196). A hairline marks the edge while more of the form is hidden under it.

### Settings (D189)
- The reminder-health line.
- **«ظاهر»:** «با همراه» or «ساده» (D200). With a companion, **«همراه»:** the character with a line in its voice, and the four pixel cards (a tap re-themes the app and the widget). In Simple, **«رنگ»:** the four palettes by color.
- **«صفحه‌ی امروز»:** list or parts.
- **«زبان»:** like the phone, فارسی, English.
- Rows to tasks, reminders, backup and phone setup; the version.
- No light mode.

The screens it opens keep their v0.x layout on the night sky, with pixel cards, buttons and calendar.

### Goals (D191)
The character with a «در دست ساخت» tag and one line; in Simple, the tab's icon, the tag and a plain line. The goal flow (`goal-step*.html`) comes after v1.

### Widget
Today's tasks for the current part, one tap each, in the character's colors.

## Language (D174–D176)

English and Persian. Persian is right to left with Persian digits and Jalali dates; English is left to right with Latin digits and Gregorian dates. The language follows the phone unless picked in Settings. The date picker is the Jalali calendar in both (D129).

## Motion

Compose animations only (D196). The full set, with timings and the Compose recipe for each, is the motion study `docs/design/today-motion.html`. With the phone's "Remove animations" on, all of it is instant: no typing, no breathing, no splash hold.
- **Today:**
  - **T1 Ticking:** the check pops, the tick draws in one pixel at a time and eight sparks fly out.
  - **T2:** the block that just filled drops in.
  - **T3, once a day at 100%:** a light runs across the blocks, two rings spread and the character hops.
  - **T4 «الان»:** the done task slides toward the past and the next one comes in from the future side.
  - **T5:** a new task glows once in its place and scrolls into view; its block grows in.
  - **T6:** mood changes crossfade; a tap squashes and hops the character.
  - **T7:** the picked part's icon moves, and a part's green square pops in when the part is all done.
  - **T8:** «+» in the bar presses in its character's way (sinks, or squashes). It no longer steps aside on scroll (D201).
  - **T9:** the leaf turns, the list changes with fades, and + shrinks away.
  - **T10:** the screen rises in once per cold start, after the splash.
- **New task:**
  - **S1:** one highlight slides between choices.
  - **S2:** sections open and close to their height; «تنظیمات بیشتر» lets its sections rise in one by one while the sheet scrolls along.
  - **S3:** the help line crossfades.
  - **S4:** the stepper number rolls.
  - **S5:** the character pops up to greet.
  - **S6:** a missing name nudges the field in amber.
  - **S7:** «ذخیره» turns green into a pixel tick while the character hops and answers.
- **The dialog box:** it types its line letter by letter.
- **The splash:** a single timeline.
- **Haptics:** a light tap on a tick, and a medium one when the day is complete.

## Not designed yet

- English screenshots of every screen (checked in L9 on the phone).
- The goal flow's place in v1.x, the goals wall, the dashboard, plugins.
