# 09 · Prioritized roadmap

_Research date: 2026-10-10. Proposals only; the user decides. IDs refer to `08` (F) and `06` (R)._

## Scoring method

Each idea gets 1–5 on four factors, and effort on a 1–3 scale:

| Factor | Meaning here |
|---|---|
| **V** User value | How much it helps the user **do the program** (adherence, proof, fewer quits). Replaces "business impact", since there is no business. |
| **C** Confidence | Strength of the evidence it works (5 = large trials or direct fact; 1 = a hunch). |
| **F** Fit | Fits the product rules (no shame, local-first, small) and the build plan. |
| **E** Effort | 1 = S (≤ 1 day) · 2 = M (a few days) · 3 = L (a week+). |

**Score = V × C × F ÷ E** (max 125). A separate **Bazaar** column notes value for a future release (–, low, med, high). These are **estimates by one researcher**, useful for ordering, not precise measurements. Two ideas within ~15 points of each other are effectively tied.

## Scored backlog

| Rank | Idea | V | C | F | E | **Score** | Bazaar | Category |
|---|---|---|---|---|---|---|---|---|
| 1 | F1 Reminders in the first installable build (re-order slices 1–2) | 5 | 4 | 5 | 1 | **100** | – | Quick win |
| 2 | R3/F5 In-session PR moment | 4 | 4 | 5 | 1 | **80** | med | Quick win |
| 3 | R2/F7 Comeback acknowledgment | 4 | 4 | 5 | 1 | **80** | high | Quick win |
| 4 | R5/F4 Pre-app days not gray + workout backfill | 4 | 4 | 5 | 1 | **80** | med | Quick win |
| 5 | R1/F2 Chapters by Jalali month + «هفته N از ۲۳» line | 4 | 4 | 5 | 1 | **80** | high | Quick win |
| 6 | R4/F6 Anchor cue («بعد از…») per task | 4 | 4 | 5 | 1 | **80** | high | Quick win |
| 7 | F12 Build check: no `INTERNET` permission | 3 | 5 | 5 | 1 | **75** | high | Quick win |
| 8 | F3 Decide "after the finish line" (maintenance vs new program) | 4 | 3 | 5 | 1 | **60** | high | Strategic (decide now) |
| 9 | F8 Local usage log (needs user OK) | 3 | 4 | 4 | 1 | **48** | low | Enabler |
| 10 | R7/F9 Tick feedback (haptic, small animation) | 3 | 3 | 5 | 1 | **45** | med | Quick win |
| 11 | F11 1-minute Friday ritual path | 3 | 3 | 5 | 1 | **45** | med | Quick win |
| 12 | F10a Photo consistency checklist (before 6 Nov) | 3 | 3 | 5 | 1 | **45** | low | Quick win |
| 13 | R6 Routine strength score → "best routines" | 3 | 3 | 5 | 1 | **45** | med | Improvement |
| 14 | R8/F2 Halfway recap at Yalda (ship by 19 Dec) | 4 | 3 | 5 | 2 | **30** | high | High-impact improvement |
| 15 | F3 Build maintenance / new-program mode (by ~week 18) | 4 | 3 | 5 | 2 | **30** | high | Long-term |
| 16 | R6 Reminder fade offer (from week ~8) | 3 | 3 | 5 | 2 | **23** | med | Improvement |
| 17 | F10b Ghost-overlay camera (before ~11 Dec check-in) | 3 | 3 | 5 | 2 | **23** | med | Improvement |
| 18 | F14 Accessibility pass | 2 | 5 | 4 | 2 | **20** | **high** | Long-term (release) |
| 19 | R10 Special days as «ذخیره» (copy) | 2 | 2 | 5 | 1 | **20** | low | Gamification experiment |
| 20 | Identity sentence | 2 | 2 | 4 | 1 | **16** | low | Gamification experiment |
| 21 | F15 Persian app name chosen with a release in mind | 2 | 2 | 4 | 1 | **16** | **high** | Long-term |
| 22 | F13 Today widget (after MVP, before week 8) | 3 | 2 | 4 | 2 | **12** | med | Improvement |
| 23 | R9 Real-world milestones | 3 | 2 | 4 | 2 | **12** | med | Gamification experiment |
| 24 | ~~Weekly share image to one friend~~ — **rejected** (D63: no sharing) | 2 | 3 | 2 | 1 | 12 | med | — |

## Grouped

### Quick wins (do inside the slices already planned)
F1 · R3 · R2 · R5 · R1 · R4 · F12 · R7 · F11 · F10a · R6 (score only)

### High-impact product improvements
R8 Yalda recap · F3 decide-then-build "after the finish line" · R6 reminder fade · F10b ghost camera

### Gamification experiments
R10 reserves copy · identity sentence · R9 milestones

### Marketing experiments (only after a release decision; see `07` §8)
"Before Nowruz" launch · Xiaomi reminder guide · 3-creator pilot · listing headline test

### Longer-term strategic investments
Maintenance / new-program mode · accessibility pass · Persian name · starter programs + onboarding for strangers (release only) · widget

### Avoid or postpone

| Idea | Decision | Why |
|---|---|---|
| Streaks, streak counters, streak freezes | **Avoid** | Product rule; broken-streak research |
| XP, levels, ranks, currencies, cosmetics | **Avoid** | Undermines intrinsic motivation; no fit |
| Leaderboards, in-app social, accounts | **Avoid** | One user; needs servers; out of scope |
| Variable / random rewards | **Avoid** | Attention-capture pattern |
| Loss mechanics (HP, restart, deposits) | **Avoid** | Product rule; cheating; effects end with incentives |
| AI coach, AI body analysis | **Avoid** | Out of scope; needs foreign servers |
| Subscriptions | **Avoid** | Needs accounts/servers; breaks during shutdowns |
| Daily quests / weekly challenges | **Postpone** | Only if boredom appears despite chapters |
| Narrative/fiction layer | **Postpone** | Content cost; chapters give the framing |
| Meal planner with recipes | **Postpone** (as planned) | Big; not tied to the top risks |
| Cloud sync to own .NET backend | **Postpone** (as planned) | Syncthing covers backup |

## 30 / 60 / 90 days

Assumes the slice plan in `ARCHITECTURE.md` §14 and roughly one slice per 1–2 weeks of evening work. That pace is a guess; adjust to reality.

| Window | Calendar | Build | Add from this research | Decide |
|---|---|---|---|---|
| **Next 30 days** | to ~9 Nov (week 5) | Slices 1–2 (Today + reminders), reordered so reminders come first (F1) | R7 tick feedback; R1 «هفته N از ۲۳» line; R5 `installDate`; F12 manifest check; F10a photo checklist **before Fri 6 Nov** | F8 local usage log: yes/no. R4 anchors: yes/no. |
| **Days 31–60** | to ~9 Dec (week 9) | Slices 3–5 (tasks editor, workout, dashboard) | R4 anchor field; R3 PR moment; R2 comeback line; R5 pre-app cell style | F3 "after the finish line" shape |
| **Days 61–90** | to ~8 Jan (week 13) | Slices 6–8 (backup, progress/check-in, Friday) | **R8 Yalda recap by 19 Dec** (numbers-only fallback); R2 comebacks in Friday wins; R6 best routines; F10b ghost camera before the Azar check-in (~11 Dec, if the default check-in rule holds); F11 short ritual path | Keep, change or stop each experiment in `10` that has run 4+ weeks |

After day 90: onboarding (slice 9), R6 reminder fade (week 8+ data), widget, then build F3 by ~week 18 (late February).

## Metrics for the whole program (from local data; no baselines exist yet)

| Metric | Source | Why |
|---|---|---|
| **Show-up days per week** (level ≥ 1) | day snapshots | Primary adherence measure; never resets |
| Weekly score % | day snapshots | Completeness |
| Comeback rate · two-day gaps per month | day snapshots | Recovery after misses (R2) |
| Reminders on time (health log) | `reminder_log` | Reliability |
| Notification → first tick latency | F8 log | Reminder usefulness, habituation |
| Friday rituals done / weeks | `week` | Weekly fresh start |
| Check-ins on time | `check_in` | Proof cadence |
| PRs per week · strength deltas since Day 0 | `set_log`, `metric_entry` | Proof early |
| SRBAI for 3 routines at weeks 4, 12, 23 | Friday ritual question | Habit automaticity (F6) |
| Weeks active out of 23 | day snapshots | The n = 1 version of "retention" |

The user's own first 2–3 weeks with the app are the baseline. Don't compare to category averages (3–4% day-30 retention); they describe a different population.
