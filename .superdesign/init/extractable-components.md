# Extractable components

Stack: native Android, Kotlin, **Jetpack Compose** (Material 3 base, custom `Ba` theme), Navigation Compose with type-safe routes. No web framework, no Tailwind, no CSS. The HTML mockups in `docs/design/*.html` mirror the Compose UI with CSS tokens and are the design reference.

The sources are Compose, not React, so there are no JSX templates to convert. The layout pieces below exist as HTML in `.superdesign/replica_html_template/today.html` and are reproduced inline in drafts.

#### Layout Components

## BottomBar
- Source: `app/src/main/kotlin/app/beaesthetic/ui/components/BottomBar.kt` (HTML: `.nav` in the template)
- Category: layout
- Description: 64 dp bottom tab bar, 4 icon-only tabs (RTL from the right: Today, Dashboard, Progress, Week); active = text-1 + 5 dp accent dot
- Extractable props: activeItem (string, default: "today")
- Hardcoded: icons (check-circle, grid, trend line, calendar), colours, sizes

#### Basic Components

## RoundIconButton
- Source: `app/src/main/kotlin/app/beaesthetic/ui/components/Common.kt` (HTML: `.round`, `.round.acc`)
- Category: basic
- Description: 44 dp circular icon button; accent variant for «+»
- Extractable props: none

## DayRing
- Source: `app/src/main/kotlin/app/beaesthetic/ui/components/Common.kt` (HTML: `.ring`)
- Category: basic
- Description: progress ring with done count and «از N»
- Extractable props: done (number), total (number)

## Chip
- Source: `app/src/main/kotlin/app/beaesthetic/ui/components/Common.kt` (HTML: `.chip.acc`, `.chip.nt`, `.chip.neu`)
- Category: basic
- Description: pill label in accent, amber or neutral
- Extractable props: none
