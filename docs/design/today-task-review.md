# Today and New Task — design review

Proposal for review, 2026-10-28. Preview: `today-task-review.html`. No Android changes or accepted design decisions.

Reviewed the current references (`today-v5.html`, `settings-task.html`), DESIGN.md, D186–D191, and the Compose Today/editor implementations. These are source-based findings, not results from a usability study or a device session.

| Priority | Current friction | Proposed change |
|---|---|---|
| High | Today gives the companion a 128 dp portrait in Compose (the HTML hero is 196 px tall), before progress and the next action. | Compact the companion to 72 px, shorten the dialogue, and keep the next action high on the screen. Keep the personality and pixel identity. |
| High | The list shows the focus task in both the Now card and its day section. | Show it once in the focus card, then list the remaining tasks. Keep its day-part label visible. Add a direct Undo after completion; existing rows can already be toggled back. |
| High | “Today”, “Tomorrow” and “Every day” mix date and recurrence in one choice group. Part of day and reminder controls need the full form. | Separate start date, part, repetition and reminder. Keep the name as the only required typed field. Preserve the add-entry context when choosing defaults. |
| High | Compose puts Save inside the scrolling column and waits 900 ms before invoking onSave. | Keep the action outside the scroll area and above the keyboard using IME insets. Save first, then show a brief confirmation; the companion response must not delay persistence. Prototype footer demonstrates placement, not native keyboard behavior. |
| Medium | Minimum day is an icon; completed rows compete with open tasks; metadata is small and low emphasis. | Label minimum day, explicitly explain its scope, fold completed tasks, improve secondary text contrast, and use at least 48 px interaction targets in the prototype. Test final colors and large font scaling in Android. |
| Medium | The parts layout has less of the “do this now” emphasis than the list. | Keep the shared next-step card in both, and make day-part names explicit. Prototype taps work; swipe behavior is still an Android implementation detail. |

The proposal retains Estedad, the four companions, the night sky, pixel corners, green completion, amber informational notices and no missed-task shame. It does not change the separate typography exploration.

The prototype supports Persian/RTL and English/LTR, theme selection, list/parts views, minimum day, completion and undo, visible scheduling controls, reminders, optional fields and adding tasks. It uses fixed sample “today” (October 28, 2026 at 08:20). New dated tasks appear on Today only when scheduled for that sample date; undated tasks appear under Anytime. State is in memory and resets on reload. Recurring reminder delivery and calendar progression are not simulated. The date control uses a native browser picker; keep the Jalali picker in Android. The Goals label indicates its existing coming-soon status.

Validation: JavaScript syntax, embedded asset references and unique IDs checked. Browser visual review could not run because Chrome's extension was disconnected after retry. Layout, actual touch behavior, screen-reader output and keyboard clearance still require browser/device verification. No APK was built because this is a design proposal only.
