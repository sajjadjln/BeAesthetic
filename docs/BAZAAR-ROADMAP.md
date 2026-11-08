# Lantern on Cafe Bazaar: roadmap to the first release

_Written 2026-11-03 on the `lantern` branch. Questions answered the same day (D199). Work happens on the `bazaar` branch (D207); B1 built 2026-11-04. Updated 2026-11-06 from Bazaar's developer docs (D217). The full list of items is `STORE-RELEASE-CHECKLIST.md`; this file puts them in order. The listing text, reviewer notes and the art letter are in `BAZAAR-LISTING.md`._

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

### B1. Flavors ✅ built 2026-11-04 on branch `bazaar` (D207)
_Done: flavors `personal` (`app.lantern.me`) and `bazaar` (`app.lantern`); the Bazaar build has Persian as `values/` (copied from `values-fa/` at build time), `fa` only, the language fixed (`BuildConfig.ONLY_LANGUAGE`), no language switch, one launcher alias; `BazaarStringsTest` fails on any string with Latin letters (only "Syncthing" is allowed). `check` lints both; `FLAVOR=bazaar scripts/release.sh` builds the signed Bazaar APK without installing it. Not yet seen on a phone._
- Product flavors `personal` and `bazaar` (`play` later). `personal` keeps today's build; it gets its own ID (Q4).
- `bazaar`: `localeFilters = ["fa"]`, Persian becomes `values/`; the language switch is hidden; one launcher alias; `app_name` «فانوس» only, no "Lantern" in any string.
- `check` builds and lints both flavors; a test reads the Bazaar APK and fails if an English string is inside.

### B2. Clean release build ✅ (done 2026-11-05, D212)
- ✅ `CAMERA`, `android.hardware.camera.any`, CameraX and the camera `FileProvider` removed. The merged release manifest now asks for: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM`, `VIBRATE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (ours), and `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` (WorkManager's; kept, see D212). The no-`INTERNET` check stays on.
- ✅ The debug menu was already out of release (`src/release/.../DebugMenu.kt`); `Log.d`/`Log.v` are stripped by R8 (`proguard-rules.pro`).
- ✅ The signed release APK (R8 on) was walked on the emulator: splash, onboarding, Today (both layouts), add/edit/tick/hold-to-delete, History, Settings and its sub-screens, «درباره», backup and restore, the widget (shows and ticks), a test reminder and a task alarm with its «انجام شد» action. R8 broke nothing.

### B3. Data that never gets lost ✅ (done 2026-11-05, D213)
- ✅ `MigrationTest` (`MigrationTestHelper`, Robolectric): every schema in `app/schemas` migrates to the newest with real-looking rows and opens through Room; 1 → 2 has its own test. Runs in every `./gradlew check`.
- ✅ `BackupSamplesTest`: one sample zip per shipped format and schema in `app/src/test/resources/backups/` (a real 0.9.4 backup, and a schema-1 one derived from it; its README says how). Each restores into the newest schema with every row intact. The test fails when a new schema or format ships without a sample.
- ✅ Settings → «پاک کردن همه‌چیز»: a page that offers a backup first (status and «همین الان پشتیبان بگیر», or pick a folder), then a plain confirmation. Erases the database, preferences, crash log, alarms and notifications, then opens onboarding. Backups in the folder stay.
- ✅ No good backup in 7 days: one calm line under «پشتیبان» in Settings. Today already has its own backup row (no folder, a failed try, or more than 10 days), unchanged.

### B4. Updates and the store links
- ✅ **Done 2026-11-05 (D214).** Bazaar's update service is called directly over its AIDL (no library, no `INTERNET`); «بررسی به‌روزرسانی» in Settings, a once-per-version line on Today, «تازه‌ها» from `whats_new_<versionCode>`, the rating ask (`StoreRules` in `:domain`), «امتیاز بده تو بازار» in «درباره».
- BazaarUpdater: «نسخهٔ تازه آمده» as one quiet line in Settings and once on Today, with a button to the Bazaar page. Verify the library first; if it's gone, the button alone («بررسی به‌روزرسانی» opens the Bazaar page).
- «تازه‌ها» (what's new) sheet, once after each update. The text lives in resources per version.
- «امتیاز بده»: `ACTION_EDIT` on `bazaar://details?id=app.lantern`. One ask, after the 7th day with ticks, never after a gap, "don't ask again" honored.
- No link to Google Play anywhere in the Bazaar build.

### B5. «درباره» (About) ✅ (done 2026-11-05, D211, D215)
- ✅ Version, the privacy note in one paragraph, and a contact row. The row stays hidden until you put an email or a link in `about_contact` (`app/src/main/res/values/strings.xml`, not translated).
- ✅ **Crash log** (D211): an `UncaughtExceptionHandler` keeps the last 20 stack traces, one file each; «درباره» shows them with «کپی». They go in the backup. Nothing is sent.
- ✅ «مجوزها» (D215): Estedad (OFL), every library in the release build (Apache 2.0, and BSD 3-Clause for the Protocol Buffers copy inside Glance), each with its full text; the character pictures as "used with permission". The list is hand-kept in `LicensesScreen.kt`; refresh it when a dependency changes.
- ✅ Contact set 2026-11-06: `donniedoorkar@gmail.com` (already skipped by `BazaarStringsTest`). «درباره» names the maker, «ساخته‌ی دانی دورکار» / “Made by Donnie Doorkar”, and so do the privacy note and «مجوزها» (D217).

### B6. Other people's phones (code done 2026-11-06, D217)
- ✅ Phone setup for Samsung and Huawei/Honor next to Xiaomi (`PhoneBrand` in `:domain`): Huawei/Honor get the autostart row («راه‌اندازی برنامه»: search the app, turn off automatic management); Samsung's battery row opens the app's page («باتری» → «بدون محدودیت»). The onboarding step no longer names Xiaomi; the autostart row says to search for the app's name; the battery row ends with «بذارش روی «بدون محدودیت»».
- ✅ TalkBack: no tappable item without a spoken name on onboarding, Today, the task sheet, Settings, «رسیدن یادآورها» and «درباره» (a UI dump on the emulator found none; not tried with TalkBack itself).
- ✅ Font scale 1.3× on Today and the task sheet: fine in Persian on the emulator (chips wrap, nothing cut).
- **Needs a device:** a real Samsung and a Huawei/Honor (the component names of their autostart screens are not public and change between versions; the fallback is the app's details page). Android 8 on an emulator; the newest Android.
- **Needs a device:** reminder health check on a non-Xiaomi phone that blocks alarms.

### B7. Beta (Q5), through Bazaar's alpha release (D217)
- Needs the Bazaar account first (B8's first step moves here).
- Upload the signed Bazaar APK as an **alpha release** («رهانش آلفا»): a group of 3–5 testers by mobile number (≤100 per group); they log in to Bazaar with that number and install and update through Bazaar. It is reviewed like any release. Two weeks. This also tests the real update check and the rating intent, which a hand-sent APK can't.
- Watch for: reminders that didn't arrive, onboarding steps people didn't understand, crashes they paste from «درباره».
- Fix, bump the version, upload again to the same alpha group; repeat once if needed. Then switch the release to «عمومی» (it can't go back to alpha).

### B8. Listing and publish (text ready in `BAZAAR-LISTING.md`)
- **You:** the developer account on the Bazaar panel (national ID, a mobile in your own name, two-step login, the yearly subscription), and confirm `app.lantern` is free. Developer title «دانی دورکار» / “Donnie Doorkar”. Do this before B7.
- No art letter: the character art is the user's own work (D218). Only Estedad ships, so no font letter.
- No privacy-policy URL for Bazaar (the in-app note is enough); keep it for Google Play.
- Listing: title **«فانوس»** only (it must equal the launcher label); search words in the short description; description ≤800 chars with no future features; category **«ابزارها»**; support email `donniedoorkar@gmail.com`; 512×512 square icon with no rounded corners or shadow; 5–6 screenshots (not the Goals tab); a 5:2 header image.
- Notes for the reviewers: a reason for every permission, WorkManager's three included, and the «در دست ساخت» Goals tab (`BAZAAR-LISTING.md` §5).
- Upload the signed APK from `FLAVOR=bazaar scripts/release.sh`, publish mode «انتشار دستی». No staged rollout (it needs 5,000 active installs).
- After launch: read install health and reviews weekly for the first month; check whether the panel has a crash view. The local crash log in «درباره» stays the main source.

## After 1.0 (not in this roadmap)
1. The goal flow (SPEC v1.0) replaces the «در دست ساخت» page.
2. The server: static files for the plugin store, with the minimum version in its JSON → hard force update.
3. The plugin store and the workout plugin.
4. Google Play (`play` flavor, English only, all four characters).
