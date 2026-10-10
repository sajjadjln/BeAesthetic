# 10 · Experiment backlog

_Research date: 2026-10-10._

## How to experiment with one user

There is no A/B testing with n = 1. These experiments use **single-case designs**, which are the standard way to test an intervention on one person:

| Design | How it works | Use when |
|---|---|---|
| **ABAB** (on/off/on/off) | Alternate the feature in fixed blocks (e.g. 2 weeks each) | The feature can be switched off without harm and its effect should appear fast |
| **Multiple baseline** | Start the change on some routines first, others later | The change applies per task |
| **Landmark comparison** | Compare the windows before and after repeated events | The intervention is tied to dates (chapters, recaps) |
| **Pre/post with a guardrail** | Turn it on and watch for harm | Effects are slow or the feature is one-way |

**Rules for all of them:**
- **Baseline** = the user's own first 2–3 weeks with the app. No invented numbers.
- **Decide on practical thresholds, not p-values.** With one person and 23 weeks, "about one more show-up day a week, sustained for 3+ weeks" is meaningful; a 3% change is noise.
- **Self-report counts.** The user's sense of "this helps / this annoys me" is a primary signal, recorded in one line in the Friday ritual.
- **Toggles** for ABAB features live in the debug menu (already planned in `ARCHITECTURE.md` §4).
- Most metrics need only the day snapshots; E4 and E5 need the local usage log (F8).

Life events (exams, illness, travel, Nowruz shopping) will swamp small effects. Note them in the Friday review so they can be excluded.

---

## E1. Anchor cues build habits that outlive reminders
| | |
|---|---|
| **Hypothesis** | Adding a «بعد از…» anchor raises automaticity and adherence of a routine, compared with clock-time reminders alone. (Gollwitzer & Sheeran 2006; Stawarz 2015) |
| **Target** | 4 routines that slip: morning skincare, night skincare, night brushing, snack prep |
| **Change** | Anchor text in the task and in its block notification |
| **Design** | **Multiple baseline**: anchors on 2 routines (picked by coin flip) from week A; on the other 2 three weeks later |
| **Primary metric** | % of due days done per routine (2-week rolling) |
| **Secondary** | SRBAI (4 items, 1–7) per routine at start, +3 weeks, +6 weeks |
| **Guardrails** | No increase in notification taps needed per tick; no drop in overall show-up days |
| **Period** | 6 weeks |
| **Dependencies** | R4 (task field + notification text); slice 3 |
| **Decide** | **Continue** for all routines if anchored routines rise ≥ 10 points vs their own baseline while un-anchored ones don't, or SRBAI rises ≥ 1 point. **Change** wording if no difference. **Stop** if the user finds it noise. |

## E2. Comeback acknowledgment reduces two-day gaps
| | |
|---|---|
| **Hypothesis** | Recognizing the first tick after a missed day reduces two-day gaps. (Milkman 2021; Silverman & Barasch 2023) |
| **Change** | «خوش برگشتی» on the first tick after a level-0 day; comebacks listed in Friday wins |
| **Design** | **ABAB**, 3-week blocks (misses are rare, so blocks are longer) |
| **Primary metric** | Comeback rate = level-0 days followed by a level ≥ 1 day ÷ level-0 days |
| **Secondary** | Two-day gaps per block |
| **Guardrails** | The user doesn't report it as patronizing |
| **Period** | 12 weeks |
| **Dependencies** | R2; slices 1 and 8 |
| **Decide** | Misses may be too rare to compare; if there are fewer than 3 level-0 days in total, **keep it on** (low cost, no harm) and rely on self-report. **Stop** if it feels patronizing. |

## E3. Chapters create fresh starts
| | |
|---|---|
| **Hypothesis** | Show-up days rise in the 14 days after a chapter start, compared with the 14 days before. (Dai 2014; Bonezzi 2011) |
| **Change** | Chapters by Jalali month with a start moment on Today |
| **Design** | **Landmark comparison**, replicated at each chapter boundary (Azar, Dey, Bahman, Esfand) |
| **Primary metric** | Mean show-up days/week, 14 days after vs before each boundary |
| **Guardrails** | Weeks that were already strong (≥ 6 show-up days) don't drop after the boundary (Dai 2018: resets can demotivate people doing well) |
| **Period** | 4 boundaries ≈ 16 weeks |
| **Dependencies** | R1; slice 1 (line) and 7 (chapter start card) |
| **Decide** | **Continue** if ≥ 3 of 4 boundaries show a rise, or the user says it helps. **Change** (quieter start) if strong weeks drop after boundaries. |

## E4. Rotating notification wording slows habituation
| | |
|---|---|
| **Hypothesis** | Varying the block-notification text (same facts, 3–4 phrasings) keeps notification → first tick latency from creeping up. (HeartSteps habituation; Duolingo template bandit) |
| **Change** | Rotate between neutral Persian templates |
| **Design** | **ABAB**, 2-week blocks: fixed text vs rotated |
| **Primary metric** | Median minutes from block notification to first tick in that block (F8 log) |
| **Guardrails** | No guilt words in any template; no extra notifications |
| **Period** | 8 weeks |
| **Dependencies** | F8 local usage log; slice 2 |
| **Decide** | **Keep rotation** if latency is lower in both rotated blocks. Otherwise **keep fixed** (simpler). |

## E5. In-session PR moment makes sessions feel better
| | |
|---|---|
| **Hypothesis** | Showing «رکورد» on the set that beats the previous best raises session enjoyment and doesn't hurt completion. (Adams 2017; Teixeira 2012) |
| **Design** | **ABAB**, 2-week blocks (≈ 6 sessions each) |
| **Primary metric** | One-tap post-session rating (😐 / 🙂 / 😀) |
| **Secondary** | Sessions finished / scheduled; sessions started within 2 h of the Evening reminder (F8) |
| **Guardrails** | Effort ratings don't drift to 10 (chasing PRs past good form) |
| **Period** | 8 weeks |
| **Dependencies** | R3; slice 4 |
| **Decide** | **Keep** unless ratings are no better *and* the user doesn't care. Cheap to keep. |

## E6. Fading reminders for established routines
| | |
|---|---|
| **Hypothesis** | A routine whose strength score has stayed high for 4+ weeks keeps ≥ 80% adherence after its reminder is silenced. (Renfree 2016; Loop score) |
| **Change** | Offer to silence that routine's reminder (stays in Today and in the block count) |
| **Design** | **Pre/post with guardrail**, per routine |
| **Primary metric** | % of due days done in the 2 weeks after silencing |
| **Guardrail** | If < 80% for 7 rolling days, the app offers to turn the reminder back on |
| **Period** | From week ~8, ongoing |
| **Dependencies** | R6 (score + offer) |
| **Decide** | **Continue** per routine while ≥ 80%; **restore** otherwise. If most routines fail the guardrail, raise the threshold. |

## E7. Short Friday ritual raises completion
| | |
|---|---|
| **Hypothesis** | A 1-minute path (review → start the week; plan steps optional) raises the share of weeks with a completed ritual. (Krebs & Duncan 2015: data entry is the top quit reason) |
| **Design** | **Pre/post**: 4 weeks with the full ritual, then 4 with the short path available |
| **Primary metric** | Rituals completed / weeks |
| **Guardrails** | Special days and snack notes still get planned in ≥ 50% of weeks |
| **Period** | 8 weeks |
| **Dependencies** | F11; slice 8 |
| **Decide** | **Keep** if completion rises or stays at 100% with less time. |

## E8. Halfway recap at Yalda
| | |
|---|---|
| **Hypothesis** | A recap at the halfway point (week 11) lifts show-up days in the 3 weeks after it, compared with the 3 weeks before. (Bonezzi 2011; Strava Year in Sport) |
| **Design** | **Landmark comparison** (one shot). Yalda (30 Azar) is the day before the Dey chapter starts, so make the recap **the Dey chapter start** and read E3's Dey boundary and E8 as one event. Compare with the other chapter boundaries to see whether the recap added anything. |
| **Primary metric** | Show-up days/week, 3 weeks after vs before 21 Dec |
| **Guardrails** | The recap leads with positives; the user doesn't report feeling worse after it |
| **Period** | 6 weeks around Yalda |
| **Dependencies** | R8; **ship by 19 Dec 2026** |
| **Decide** | Informs the finish recap: **reuse** the format if it lifted mood or show-up; **simplify** if it felt like homework. |

## E9. Special days framed as reserves (optional)
| | |
|---|---|
| **Hypothesis** | Calling special days «روز ذخیره» makes the user use them more deliberately and rebound faster after one. (Sharif & Shu) |
| **Design** | **Pre/post**, 6 weeks each |
| **Primary metric** | Special days used per week; show-up the day after a special day |
| **Guardrails** | Special days aren't avoided when genuinely sick (self-report) |
| **Dependencies** | R10 (copy only) |
| **Decide** | Keep whichever wording the user prefers; the evidence is too thin to override preference. |

## E10. Weekly share image to one friend (only if the user wants it)
| | |
|---|---|
| **Hypothesis** | Sending the week's wins to one friend raises show-up days. (Harkin 2016: public reporting strengthens monitoring; STEP UP support arm) |
| **Change** | Friday ritual can export a wins-only image; the user sends it themselves (no accounts, no server) |
| **Design** | **ABAB**, 3-week blocks: sending vs not |
| **Primary metric** | Show-up days/week |
| **Guardrails** | The user never feels pressure to hide a bad week (wins-only image) |
| **Dependencies** | Changes SPEC §3.3 ("no sharing"); needs the user's decision |
| **Decide** | **Keep** only if both "on" blocks beat both "off" blocks and the user likes it. |

---

## Order to run them

E1 and E4 can run at the same time as E5 (different features, different metrics). Don't run two ABAB experiments on the same metric (show-up days) at once.

| Weeks (approx.) | Running |
|---|---|
| 5–11 | E1 (anchors), E4 (wording), E5 (PR moment) |
| 8+ | E6 (fading), ongoing |
| 9–17 | E3 (chapters, at each boundary), E7 (ritual) |
| 10–14 | E8 (Yalda) |
| 12–23 | E2 (comebacks), E9 or E10 if chosen |
