# Store release checklist (later, not now)

Lantern is personal for now (D49, D173). This is the list to work through when it goes to Cafe Bazaar or Google Play. Each store gets **one language** (D195). Add to this list whenever something comes up that only matters for a store release. The order for Bazaar is `BAZAAR-ROADMAP.md`.

**How:** two product flavors, `bazaar` and `play`, in the one codebase. No long-lived branches. Each flavor sets its own `localeFilters`, its characters and its listing; `personal` (today's build, both languages, all four characters) stays the default.

## Both stores

- [ ] Product flavors `personal`, `bazaar`, `play`; CI/`check` builds all three.
- [ ] One language per store: hide the language switch in Settings, drop the three launcher aliases down to one (D194 ④), and drop the date order and digits code paths the store doesn't use.
- [ ] The app starts empty; nothing personal is seeded (D100). Remove the debug menu from release (`DebugMenu.AVAILABLE`).
- [ ] Remove leftover permissions from «پیکر»: `CAMERA` (the check-in camera is gone; still in the manifest on 2026-11-03). Re-check every permission in the merged manifest.
- [ ] `INTERNET` only if the plugin store ships (D164, D167); otherwise keep the no-`INTERNET` check.
- [ ] Privacy note: the app collects nothing; the usage log never leaves the phone (D59). Both stores want a privacy policy URL.
- [ ] Same release key as the personal install (ARCHITECTURE §13), so a store build can update it. On Play this means **Play App Signing with your own key uploaded**, not a Google-generated one.
- [ ] Version 1.0.0 only after the L9 checks (LANTERN-ROADMAP.md): a week of use, backup → reinstall → restore, reminders after reboot and battery saver.
- [ ] Reminder checklist on at least one non-Xiaomi phone too.
- [ ] Listing: screenshots of Today (both layouts), the task sheet, the splash; a short video of the splash is optional.

### Updates (added 2026-11-03, open: see TASKS.md)

- [ ] **Soft update:** tell the user a newer version is out, with one button to the store page. Never blocks anything.
- [ ] **Force update:** a minimum-version flag. Below it, the app shows one calm screen with an update button. **Even then, «پشتیبان‌گیری» (export a backup) still works on that screen** (never lose data). No red, no countdown.
- [ ] Where the flag lives. The app is offline (D164), so it can't ask our own server. Proposal: the store client tells us (Bazaar: `BazaarUpdater` talks to the Bazaar app, so our app needs no `INTERNET`; Play: In-App Updates). The hard minimum comes from the plugin store's index once `:store` exists (D167). Verify the Bazaar library still exists and what it reports.
- [ ] **What's new** («تازه‌ها»): one sheet after an update, shown once.
- [ ] **Old data always opens:** Room migrations from every version ever shipped, tested; never `fallbackToDestructiveMigration`. A backup from any older version restores into the newest.
- [ ] Never ship a version that can't be rolled back without data loss: keep the schema change and the feature that needs it in separate releases when possible.

### Quality without SDKs (D63)

- [ ] **Local crash log:** an `UncaughtExceptionHandler` writes the stack trace to a file (last 20). Settings → «درباره» shows it with a «کپی» button, so a user can paste it in a message. It goes in the backup too. Nothing is sent.
- [ ] Bazaar's developer panel shows crashes and ANRs from its own client; read it after each release.
- [ ] Test the release build (R8 on) end to end, not only debug: reflection in serialization, Room, the widget, receivers.
- [ ] Test on Android 8 (minSdk 26) and the newest Android, and on a Samsung (the most common non-Xiaomi phone in Iran). Battery guides for Samsung and Huawei/Honor next to the Xiaomi one.
- [ ] TalkBack labels on every tappable thing, and big font sizes (1.3×) don't break Today or the task sheet.

### Legal and content

- [ ] **Rights to the character art** (`characters/`) and the per-character fonts (`characters/fonts/`): confirm each may be shipped in a public, possibly paid, app.
- [ ] Estedad is under the OFL: ship its license. An «مجوزها» screen in Settings → «درباره» lists Estedad and every open-source library (AndroidX, Kotlin, Room…).
- [ ] Settings → «پاک کردن همه‌چیز» (erase all data), with a backup offered first.
- [ ] Settings → «درباره»: version, contact (an email or a Telegram/Eitaa channel you own), privacy note.
- [ ] Turn off any debug logging in release (`Log.d` stripped by R8 rules).
- [ ] `applicationId` `app.lantern` is permanent once published. Check it's free on Bazaar before the first upload.
- [ ] The release key: two backups in two places (the file and the password). Losing it means a new app and every user starting over.

## Cafe Bazaar

- [ ] **Persian only.** Remove English: `localeFilters = ["fa"]`, no `values/` English copy shipped (Persian becomes the default `values/`).
- [ ] **All four characters, the goth woman included** (D199, reverses that part of D195). Check her lines read well in Persian.
- [ ] **The app's name is Persian: «فانوس».** The launcher label, the splash word, notifications and the Bazaar listing all say «فانوس»; no "Lantern" anywhere in the Bazaar build (one launcher alias, `app_name` in Persian only).
- [ ] Persian title with the words people search for, Persian description.
- [ ] Check Bazaar's current rules on target SDK and on `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- [ ] Bazaar takes an APK; the signed APK from `scripts/release.sh` is the upload.
- [ ] Developer account on the Bazaar panel (identity check with a national ID; done by you).
- [ ] Store links go to Bazaar only: the update button and «امتیاز بده» open `bazaar://details?id=app.lantern` (rating: `ACTION_EDIT` on the same URI, package `com.farsitel.bazaar`). No link or mention of Google Play anywhere in the Bazaar build.
- [ ] A rating ask, at most once, after a good moment (e.g. the 7th day with ticks), never after a gap. Easy to dismiss forever.
- [ ] Privacy policy page in Persian at a URL you own (a static page is enough).
- [ ] Listing assets: 512×512 icon, Persian screenshots, a cover image, category (likely «بهره‌وری» or «سبک زندگی»), age rating.
- [ ] Review notes for Bazaar's reviewers: why exact alarms and the battery exception are needed (reminders on Xiaomi).
- [ ] If the workout plugin ships in the Bazaar build, check whether Bazaar files it as health and asks for a license.
- [ ] Later, only if something is paid (plugins): Bazaar's in-app billing (Poolakey). Not for 1.0.

## Google Play

- [ ] **English only.** Remove Persian: `localeFilters = ["en"]`, no `values-fa/`. The calendar shows Gregorian only (no Jalali line).
- [ ] All four characters stay.
- [ ] Upload an **AAB** (`bundleRelease`), not an APK.
- [ ] `USE_EXACT_ALARM` is limited on Play to alarm and calendar apps; declare it in the Play Console or switch to `SCHEDULE_EXACT_ALARM` only, with the system switch asked for in onboarding.
- [ ] `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` needs a Play policy reason; be ready to drop it and send people to the battery settings screen instead.
- [ ] Data safety form: no data collected, no data shared.
- [ ] Target SDK at Play's current minimum.
- [ ] Plugins stay interpreted scripts; no downloaded dex, jar or `.so` (D173).
- [ ] Still no Google Play Services inside (D63).
