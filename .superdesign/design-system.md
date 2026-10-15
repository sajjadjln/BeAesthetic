# پیکر (BeAesthetic) — design system for Superdesign

Source of truth in the repo: `docs/DESIGN.md` (visual) and `docs/DECISIONS.md` (newest entry wins). This file is a standalone digest for design generation.

## Product context

- A personal **Android** app (Jetpack Compose) for one person running a 23-week body-transformation program (9 Oct 2026 → 20 Mar 2027, ending on the eve of Nowruz).
- Every day it says what to do (routine tasks grouped in four parts of the day), logs workouts and body progress, and reminds reliably.
- **Persian only, RTL, Persian digits** (۰۱۲۳…). Jalali dates first. The week starts Saturday. The day rolls over at 04:00.
- The user quits when bored, when progress isn't visible fast, or when there's no clear goal. Routines (skincare, brushing, nails, snack prep) slip.
- Phone: Poco X4 5G, ~393 × 873 dp. One-handed use.

## Non-negotiable UX rules

1. **Today first.** The home screen answers "what do I do now?" The tasks for the current part of the day must be visible on the first screen without scrolling.
2. **One tap to log.** A task is ticked with one tap on its row/tile. No confirmation.
3. **No shame mechanics.** Nothing is ever red. No streaks that reset. No list of what was missed. No guilt wording. A missed task or day is a neutral gray gap. Earlier unticked tasks never follow you into the current part as "overdue".
4. **Calm.** Motion only confirms an action or shows progress. No badges begging for attention.
5. Tap targets ≥ 44 dp. The main action sits low on the screen (thumb zone).

## Today: content that must exist

- Header: weekday (large), the line «۲۹ مهر · هفته ۲ از ۲۳», a **leaf** button (minimum day) and a **gear** (settings).
- Day progress: done / total for today (e.g. «۱ از ۱۴»), as a ring or another compact form; 7 **week dots** (Saturday on the right) coloured by day level, today outlined, future days outlined only.
- **«پیکر»**: the app's cartoon character (a green sticker guy) with one short line in a speech bubble that follows the day. Keep him: he is the app's personality.
- **«تمرین امروز» card** on a training day: workout name (e.g. «Upper B»), «۵ حرکت، ۱۶ ست», a tick circle and a ▶ that opens the session.
- **«الان · صبح»**: the current part of the day (from the clock), «تا ۱۳:۰۰», its count, and its tasks. Each task has a check circle, a name, a small icon; «مهم» (important) and «موعدش رسیده» (due) are amber, always with the word.
- A hint of what comes next: «بعدی: ظهر از ۱۳:۰۰ · ۳ کار».
- **«همه‌ی کارها»**: the whole day one tap away (morning, midday, evening, night, each with its count) plus «هر وقت» (undated to-dos that never count).
- **«+» new task** button (green, round).
- Bottom tab bar, icons only, RTL order from the right: Today (check-circle, active with a small accent dot) · Dashboard (grid) · Progress (trend line) · Week (calendar).
- Quiet rows when needed: reminder health («یه یادآور دیر رسید · درستش کن») and backup — amber/neutral, never red.

Sample day (Wednesday 29 Mehr, 08:10, training day). Morning: آب ناشتا (done), مسواک, ضدآفتاب, ویتامین D (مهم), plus the workout. Midday: ناهار پروتئینی, میان‌وعده, امگا ۳. Evening: دوش, ناخن (موعدش رسیده), آب ۲ لیتر. Night: مسواک شب, اسکین‌کر شب, میان‌وعده‌ی فردا. Anytime: نوبت آرایشگاه, خرید پودر پروتئین.

## Visual direction: "quiet strength"

- **Dark first** (light theme exists). Flat surfaces, no gradients, no glassmorphism, no neon glow.
- **One accent = done/progress, nothing else.** Default accent green.
- Amber = due or important, always with words. Indigo = special day. Gray = gap. **No red.**
- Visual over text: icons, rings, big numbers, minimal helper text.

## Tokens (dark, "slate", green accent)

| Token | Value | Use |
|---|---|---|
| bg | `#1B201E` | Screen background |
| surface | `#252B29` | Cards, tiles, sheets |
| surface-2 | `#2F3633` | Icon wells, chips, secondary controls, speech bubble |
| border | `#3C4441` | Dividers, outlines, empty check circles |
| text-1 / 2 / 3 | `#EEF1EF` / `#A1ABA7` / `#828C88` | Text levels |
| accent | `#34C47A` | Primary button, ring, check fill, active tab dot |
| accent-subtle | `#29503C` | Done tile/row fill, positive chip bg |
| on-accent-subtle | `#5BD994` | Text/icons on accent-subtle |
| on-accent | `#0E1110` | Text/icons on accent |
| week levels 0–4 | `#3C4441` `#23513A` `#28754E` `#2FA066` `#5BD994` | Week dots, dashboard squares |
| amber / subtle | `#E8B04B` / `#4C4630` | «مهم», «موعدش رسیده» |
| special / subtle / on | `#5560C9` / `#313953` / `#A3AAF2` | Special days, minimum day |

Light theme: bg `#F6F7F6`, surface `#FFFFFF`, surface-2 `#EDF0EE`, border `#DCE1DE`, text `#0E1110` / `#5F6965` / `#8A948F`, accent `#1D8A55`, accent-subtle `#E4F7EC`, on-subtle `#176B43`, amber `#B07610` / `#FBEFD6`.

- **Spacing:** 4 · 8 · 12 · 16 · 24 · 32. Screen side padding 16.
- **Radius:** 6 (grid cells) · 12 (controls) · 16 (cards) · 20 (tiles) · 28 (sheets) · full (circles, pills).
- **Shadows:** none on cards; only floating elements (FAB, sheets) get a soft dark shadow.

## Type

Font: **Estedad** only (Google Fonts: `Estedad`, weights 400–800). Persian digits everywhere.

| Style | Size / weight / line height |
|---|---|
| Display number | 52 / 800 / 56 |
| Headline | 26 / 800 / 36 |
| Title | 18 / 700 / 28 |
| Subtitle | 16 / 600 / 24 |
| Body | 15 / 400 / 24 |
| Tile | 15 / 600 / 22 |
| Label | 13 / 500 / 20 |
| Caption | 12 / 400 / 18 |

## Components

- **Round icon button:** 44 dp circle, surface bg, text-2 icon. Primary variant: accent bg, on-accent icon.
- **Check circle:** 22–28 dp; empty = 2 dp border ring; done = accent fill with on-accent check. Amber ring for «مهم»/due when not done.
- **Task row:** check circle · name (15/500) · small text-3 icon; done → name in text-3, icon in on-accent-subtle. Min height 46 dp.
- **Card:** surface, radius 16–20, padding 12–16, no border, no shadow.
- **Chip:** pill, 12/600; neutral = surface-2 + text-2; accent = accent-subtle + on-accent-subtle; amber = amber-subtle + amber.
- **Speech bubble:** surface-2, radius 14, tail pointing at «پیکر».
- **Icons:** 24 dp line icons, 1.8 stroke, round caps/joins (Lucide-like): sunrise, sun, sunset, moon, drop, tooth, pill, egg, dumbbell, scissors, apple, shower, play, plus, chevron, list, gear, leaf, check.
- **Bottom bar:** 64 dp, bg, top border, 4 icon-only tabs, active = text-1 + 5 dp accent dot.

## Motion

Standard easing `cubic-bezier(.2,0,0,1)`; 100 / 200 / 350 / 600 ms. Tick: accent fill + check, light vibration. Only the accent moves; gray never animates.
