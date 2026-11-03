# Lantern on Cafe Bazaar: roadmap to the first release

_Written 2026-11-03 on the `lantern` branch. Questions answered the same day (D199). Not started. The full list of items is `STORE-RELEASE-CHECKLIST.md`; this file puts them in order._

## What the first Bazaar release is

«فانوس» 1.0 for strangers: the same app you use, in Persian only, with all four characters (D195, D199).

1. The splash, onboarding, Today (both layouts), the task sheet, History, Settings, reminders, backup, the widget, and the Goals tab with its «در دست ساخت» page.
2. **Persian only**, named «فانوس» everywhere. All four characters: knight, vampire, wizard, goth woman.
3. **Offline.** No `INTERNET` permission, no server. The plugin store and the goal flow come in later releases.
4. Soft update through the Bazaar app only; a hard minimum version waits for a server.

What it is **not**: the goal flow, plugins, the workout, accounts, a backend, Google Play.

## Decisions (answered 2026-11-03, D199)

| # | Question | Answer | Why |
|---|---|---|---|
| Q1 | The Goals tab in 1.0 | ✅ **Keep it**, with the «در دست ساخت» page | Your call: it shows where the app is going. |
| Q2 | Force update in 1.0 | ✅ **Soft only** (BazaarUpdater, no network) | A hard flag needs the network; nothing in 1.0 depends on a server. |
| Q3 | Server for 1.0 | ✅ **None** | Nothing in 1.0 needs one. The privacy page can live on a free static host. |
| Q4 | Your own install | ✅ **The personal build gets its own ID** (`app.lantern.me`); you move your data once with backup → restore. Bazaar gets `app.lantern` | Otherwise Bazaar sees your phone has `app.lantern`, offers an "update", and turns your build into the Persian-only one. |
| Q5 | Testing before going public | ✅ **3–5 people on different phones for 2 weeks**, with the Bazaar APK sent by hand | Reminders on phones you don't own are the biggest unknown. |
| — | The goth woman | ✅ **Stays on Bazaar** (reverses that part of D195) | Your call. |

## The slices

Each slice ends as in Phase 5: a build, "what works, what's next", your feedback. `./gradlew check` stays green and builds every flavor.

### B0. Personal 1.0.0 (already the next step)
- The L9 checks in `LANTERN-ROADMAP.md`: a week of real use on the Poco, backup → reinstall → restore, reminders after a reboot and with battery saver on.
- Tag the signed 1.0.0. **The Room schema at 1.0.0 is the baseline**: from here on every change needs a tested migration.

### B1. Flavors
- Product flavors `personal` and `bazaar` (`play` later). `personal` keeps today's build; it gets its own ID (Q4).
- `bazaar`: `localeFilters = ["fa"]`, Persian becomes `values/`; the language switch is hidden; one launcher alias; `app_name` «فانوس» only, no "Lantern" in any string.
- `check` builds and lints both flavors; a test reads the Bazaar APK and fails if an English string is inside.

### B2. Clean release build
- Remove `CAMERA`; re-check every permission in the merged manifest. The no-`INTERNET` check stays on.
- Debug menu out of release; `Log.d`/`Log.v` stripped by R8.
- Install the **release** APK (R8 on) and walk every screen: serialization, Room, receivers, the widget.

### B3. Data that never gets lost
- Migration tests from the 1.0.0 schema, run on every build.
- A backup from every shipped version restores into the newest (keep one sample zip per version in test resources).
- Settings → «پاک کردن همه‌چیز», with a backup offered first.
- If no backup was made in 7 days, a calm line in Settings (not on Today, never red).

### B4. Updates and the store links
- BazaarUpdater: «نسخهٔ تازه آمده» as one quiet line in Settings and once on Today, with a button to the Bazaar page. Verify the library first; if it's gone, the button alone («بررسی به‌روزرسانی» opens the Bazaar page).
- «تازه‌ها» (what's new) sheet, once after each update. The text lives in resources per version.
- «امتیاز بده»: `ACTION_EDIT` on `bazaar://details?id=app.lantern`. One ask, after the 7th day with ticks, never after a gap, "don't ask again" honored.
- No link to Google Play anywhere in the Bazaar build.

### B5. «درباره» (About)
- Version, a contact (an email or a channel you own), the privacy note in one paragraph.
- **Crash log:** an `UncaughtExceptionHandler` keeps the last 20 stack traces in a file; «درباره» shows them with «کپی». They go in the backup. Nothing is sent.
- «مجوزها»: Estedad (OFL) and every library's license.

### B6. Other people's phones
- Phone setup for Samsung and Huawei/Honor next to Xiaomi (their battery and autostart screens).
- Test on Android 8 (an emulator is fine), the newest Android, and a real Samsung.
- TalkBack labels on everything tappable; font scale 1.3× on Today and the task sheet.
- Reminder health check: confirm it notices when a non-Xiaomi phone blocks alarms.

### B7. Beta (Q5)
- Send the signed Bazaar APK to 3–5 people. Two weeks.
- Watch for: reminders that didn't arrive, onboarding steps people didn't understand, crashes they paste from «درباره».
- Fix, bump the version, repeat once if needed.

### B8. Listing and publish
- **You:** the developer account on the Bazaar panel (national ID check), and confirm `app.lantern` is free.
- **You:** written confirmation that the character art and the per-character fonts may be shipped publicly.
- Privacy policy page in Persian at a URL you own.
- Listing: 512×512 icon, Persian screenshots (Today both layouts, the four characters, the task sheet, History, the splash), a cover image, a Persian title with search words («فانوس | لیست کارهای روزانه و یادآور»), description, category «بهره‌وری», age rating.
- Notes for the reviewers: why exact alarms and the battery exception are needed.
- Upload the signed APK from `scripts/release.sh`. Use a staged rollout if the panel offers one.
- After launch: read Bazaar's crash and ANR panel weekly for the first month.

## After 1.0 (not in this roadmap)
1. The goal flow (SPEC v1.0) replaces the «در دست ساخت» page.
2. The server: static files for the plugin store, with the minimum version in its JSON → hard force update.
3. The plugin store and the workout plugin.
4. Google Play (`play` flavor, English only, all four characters).
