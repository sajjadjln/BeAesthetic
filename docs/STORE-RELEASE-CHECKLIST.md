# Store release checklist (later, not now)

Lantern is personal for now (D49, D173). This is the list to work through when it goes to Cafe Bazaar or Google Play. Each store gets **one language** (D195). Add to this list whenever something comes up that only matters for a store release. The order for Bazaar is `BAZAAR-ROADMAP.md`.

**How:** two product flavors, `bazaar` and `play`, in the one codebase. No long-lived branches. Each flavor sets its own `localeFilters`, its characters and its listing; `personal` (today's build, both languages, all four characters) stays the default.

## Both stores

- [ ] Product flavors `personal`, `bazaar`, `play`; CI/`check` builds all three. _(`personal` and `bazaar` done 2026-11-04, D207; `play` later.)_
- [ ] One language per store: hide the language switch in Settings, drop the three launcher aliases down to one (D194 ④), and drop the date order and digits code paths the store doesn't use.
- [x] The app starts empty; nothing personal is seeded (D100). Remove the debug menu from release (`DebugMenu.AVAILABLE`).
- [x] Remove leftover permissions from «پیکر»: `CAMERA` (removed with CameraX and the camera `FileProvider`, B2). Every permission in the merged manifest re-checked (D212).
- [ ] `INTERNET` only if the plugin store ships (D164, D167); otherwise keep the no-`INTERNET` check.
- [x] Privacy note in «درباره» (D215): the app collects nothing, no internet permission, everything stays on the phone (D59). Both stores still want a privacy policy URL (below).
- [ ] Same release key as the personal install (ARCHITECTURE §13), so a store build can update it. On Play this means **Play App Signing with your own key uploaded**, not a Google-generated one.
- [ ] Version 1.0.0 only after the L9 checks (LANTERN-ROADMAP.md): a week of use, backup → reinstall → restore, reminders after reboot and battery saver.
- [ ] Reminder checklist on at least one non-Xiaomi phone too.
- [ ] Listing: screenshots of Today (both layouts), the task sheet, the splash; a short video of the splash is optional.

### Updates (added 2026-11-03, open: see TASKS.md)

- [x] **Soft update:** tell the user a newer version is out, with one button to the store page. Never blocks anything.
- [ ] **Force update:** a minimum-version flag. Below it, the app shows one calm screen with an update button. **Even then, «پشتیبان‌گیری» (export a backup) still works on that screen** (never lose data). No red, no countdown.
- [ ] Where the flag lives. The app is offline (D164), so it can't ask our own server. Proposal: the store client tells us (Bazaar: `BazaarUpdater` talks to the Bazaar app, so our app needs no `INTERNET`; Play: In-App Updates). The hard minimum comes from the plugin store's index once `:store` exists (D167). Verify the Bazaar library still exists and what it reports. (B4, D214: it does; we call its service directly, it reports the newest version code. The force-update minimum is still open.)
- [x] **What's new** («تازه‌ها»): one sheet after an update, shown once.
- [x] **Old data always opens:** Room migrations from every version ever shipped, tested; never `fallbackToDestructiveMigration`. A backup from any older version restores into the newest (B3: `MigrationTest`, `BackupSamplesTest`).
- [ ] Never ship a version that can't be rolled back without data loss: keep the schema change and the feature that needs it in separate releases when possible.

### Quality without SDKs (D63)

- [x] **Local crash log** (D211): an `UncaughtExceptionHandler` writes the stack trace to a file (last 20). Settings → «درباره» shows it with a «کپی» button, so a user can paste it in a message. It goes in the backup too. Nothing is sent.
- [ ] Bazaar's developer panel shows crashes and ANRs from its own client; read it after each release.
- [x] Test the release build (R8 on) end to end, not only debug: reflection in serialization, Room, the widget, receivers (B2, 2026-11-05, on the emulator).
- [ ] Test on Android 8 (minSdk 26) and the newest Android, and on a Samsung (the most common non-Xiaomi phone in Iran). Battery guides for Samsung and Huawei/Honor next to the Xiaomi one.
- [ ] TalkBack labels on every tappable thing, and big font sizes (1.3×) don't break Today or the task sheet.

### Legal and content

- [ ] **Rights to the character art** (`characters/`) and the per-character fonts (`characters/fonts/`): confirm each may be shipped in a public, possibly paid, app.
- [x] Estedad is under the OFL: ship its license. An «مجوزها» screen in Settings → «درباره» lists Estedad and every open-source library (AndroidX, Kotlin, Room…), with the full texts (D215).
- [x] Settings → «پاک کردن همه‌چیز» (erase all data), with a backup offered first (B3).
- [x] Settings → «درباره»: version, contact, privacy note (D215). **You:** set the contact address in `about_contact` (`res/values/strings.xml`); the row is hidden while it's empty.
- [x] Turn off any debug logging in release (`Log.d` and `Log.v` stripped by R8 rules, B2).
- [ ] `applicationId` `app.lantern` is permanent once published. Check it's free on Bazaar before the first upload.
- [ ] The release key: two backups in two places (the file and the password). Losing it means a new app and every user starting over.

## Cafe Bazaar

- [x] **Persian only** (2026-11-04, D207). Remove English: `localeFilters = ["fa"]`, no `values/` English copy shipped (Persian becomes the default `values/`).
- [ ] **All four characters, the goth woman included** (D199, reverses that part of D195). Check her lines read well in Persian.
- [x] **The app's name is Persian: «فانوس».** _(In the build, 2026-11-04; the listing is still to do.)_ The launcher label, the splash word, notifications and the Bazaar listing all say «فانوس»; no "Lantern" anywhere in the Bazaar build (one launcher alias, `app_name` in Persian only).
- [ ] Persian title with the words people search for, Persian description.
- [x] Check Bazaar's current rules on target SDK and on `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. _(2026-11-06: target SDK ≥ 32; the three permissions aren't mentioned; every permission needs a reason for the reviewer. See below.)_
- [ ] Bazaar takes an APK; the signed APK from `scripts/release.sh` is the upload.
- [ ] Developer account on the Bazaar panel (identity check with a national ID; done by you).
- [x] Store links go to Bazaar only: the update button and «امتیاز بده» open `bazaar://details?id=app.lantern` (rating: `ACTION_EDIT` on the same URI, package `com.farsitel.bazaar`). No link or mention of Google Play anywhere in the Bazaar build.
- [x] A rating ask, at most once, after a good moment (e.g. the 7th day with ticks), never after a gap. Easy to dismiss forever.
- [ ] Privacy policy page in Persian at a URL you own (a static page is enough).
- [ ] Listing assets: 512×512 icon, Persian screenshots, a cover image, category (likely «بهره‌وری» or «سبک زندگی»), age rating.
- [ ] Review notes for Bazaar's reviewers: why exact alarms and the battery exception are needed (reminders on Xiaomi).
- [ ] If the workout plugin ships in the Bazaar build, check whether Bazaar files it as health and asks for a license.
- [ ] Later, only if something is paid (plugins): Bazaar's in-app billing (Poolakey). Not for 1.0.

### From Bazaar's developer docs (read 2026-11-06, `docs/research/bazaar-developer-guide.md` §16)

- [x] _(docs)_ Target SDK ≥ 32 (we're on 36); no min SDK rule; APK ≤150 MB accepted; `app.lantern` is a valid package name. Exact alarms, battery exception and "no internet" have no rule of their own.
- [x] _(docs)_ Rating intent (`ACTION_EDIT`, `bazaar://details?id=app.lantern`, package `com.farsitel.bazaar`) and the update-check bind action match the docs; the AIDL's first two methods match BazaarUpdater 1.2.0's order.
- [x] _(docs; done 2026-11-06, `donniedoorkar@gmail.com`)_ **Set `about_contact` before the first upload.** The contract (6-11 ز) calls missing in-app contact channels a violation; the panel requires a support email. Use the same address for both; add `about_contact` to `BazaarStringsTest`'s skip list.
- [x] _(docs)_ **Listing name «فانوس» only**: it must equal the launcher label. Search words go in «توضیحات کوتاه فارسی» (≤60 chars) and the description (≤800 chars). No future features (goals, plugins) in the description.
- [x] _(docs)_ Category **«ابزارها»** (Bazaar has no «بهره‌وری»; «ابزارها» names reminders and task apps).
- [ ] _(docs; specs in `BAZAAR-LISTING.md` §4, images still to make)_ Asset specs: icon PNG 512×512, ≤3 MB, square with no rounded corners or shadow, same as the launcher icon; 1–12 screenshots ≤1 MB each; header image PNG ≥720×288 at 5:2, ≤1 MB; optional promo images JPG 1152×648; optional Aparat video ≤10 min.
- [ ] _(docs; drafted in `BAZAAR-LISTING.md` §6, needs signing)_ **Character art letter in Bazaar's format**: written confirmation naming the developer email and `app.lantern`; from a private person also their ID details and ID copies. Upload it with the reviewer notes. Same check for the per-character fonts.
- [x] _(docs; written in `BAZAAR-LISTING.md` §5)_ Reviewer notes («توضیحات بیشتر برای بررسی‌کننده») with a reason for every permission, including WorkManager's `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`; mention the clearly labelled «در دست ساخت» Goals tab.
- [ ] _(docs; planned in B7, D217)_ Beta through Bazaar's **alpha release** (≤100 testers per group by mobile number) instead of hand-sent APKs. Staged rollout needs 5,000 active installs, so not for 1.0.
- [ ] _(docs)_ Account (you): mobile number in your own name; the email is permanent; pay the 400,000-rial yearly subscription before publishing; developer title ≤32 chars; turn on two-step login. Max 10 publish requests a day; review takes up to 3 working days.
- [ ] _(docs)_ After launch: Bazaar documents install health and reviews, **not** a crash/ANR view; check the panel for one, and keep the local crash log as the main source.
- [x] _(docs, optional)_ Update check on Bazaar older than 24.7.0 (`versionCode < 2400700`): fall back to `getVersionCode` (returns the newer code or −1). `bazaar://details/modal?id=app.lantern` opens Bazaar's in-app update sheet on newer Bazaar.
- [x] _(docs)_ Age rating: documented for games only (ESRA, optional). Skip for the app unless the panel asks. A privacy-policy URL isn't required by Bazaar (the in-app note is); keep it for Play.
- [ ] _(docs, later)_ Plugin store on Bazaar: paid content must use Bazaar billing; apps may not offer access to apps from other sources or update outside Bazaar, nor change their nature after publishing. Ask Bazaar support before shipping `:store` in the `bazaar` flavor.

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
