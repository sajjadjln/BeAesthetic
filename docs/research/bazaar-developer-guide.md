# Cafe Bazaar developer guide: a reference for «فانوس»

_Read 2026-11-06 from developers.cafebazaar.ir (Persian). The site is a Nuxt app over a WordPress API; every page was read as raw text from `https://developers.cafebazaar.ir/wp-json/v2/document/single?type=<document|rule>&slug=<slug>&lang=fa` (the sidebar tree is `.../wp-json/v2/settings?lang=fa`). Each section gives the page URL. Persian in «» is the panel's own label. "Updated" dates are the page's own last-edit dates; many rule pages date from 2021–2022, so the panel may differ. Where the docs say nothing, this file says "not documented"._

Contents: 1 Account and fees · 2 The app file · 3 Upload and listing · 4 Review and release · 5 Alpha and staged rollout · 6 Updates and the update check · 7 Intents and deep links · 8 Ratings and reviews · 9 Rules: content, quality, security, privacy, copyright, deception, ads, payment · 10 Permissions · 11 Enforcement · 12 Panel stats and APIs · 13 In-app billing (Poolakey) · 14 Money · 15 Pages not covered · **16 What this means for Lantern**

---

## 1. Account, contract, fees

Sources: [sign-up](https://developers.cafebazaar.ir/fa/guidelines/getting-started/sign-up) (updated 2025-01-07), [app-release](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/app-release) (2024-12-17), [contract](https://developers.cafebazaar.ir/fa/guidelines/getting-started/contract) (2023-12-18), [developer-title](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/overview/developer-title), [two-step-verification](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/two-step-verification).

- Register at `https://pishkhan.cafebazaar.ir/account/register` in current Chrome or Firefox. Password: at least 8 characters, one capital letter, one digit. **The email can never be changed.** The confirmation link lasts 1 hour.
- Account type «شخص» (person) or «شرکت» (company); **can never be changed**. A company account needs a company email.
- Person needs: full name exactly as on the national card, national code, 10-digit postal code, full address, a clear colour photo of the national card (or the new birth certificate's first page). **The mobile number must be registered to the same national code.** The national code must not already have a developer account.
- «اطلاعات ناشر» (publisher info) is reviewed within 12 hours. It must be «تاییدشده» before a release can go out and before any settlement.
- **Yearly subscription: 400,000 rials**, paid under Settings → «وضعیت اشتراک». You can submit an app for review without it, but you must pay before publishing; «انتشار خودکار» (auto publish) doesn't work while unpaid.
- «عنوان توسعه‌دهنده» (developer title): shown under the app name; unique; Persian and English, max 32 characters each (letters, digits, space, half-space). Up to 5 titles per account; titles can be edited, not deleted. Each title gets a one-time «شناسه‌ی توسعه‌دهندگی» (developer ID) that should match the English name in «پروفایل عمومی».
- Two-step login: Settings → «تایید دومرحله‌ای», with an authenticator app (Bazaar's page names Google Authenticator). Recommended for every owner and operator.
- The contract («قرارداد عرضهٔ برنامه و محتوای دیجیتال») must be accepted before the panel works. One year, renews itself; the developer may end it with 10 days' written notice. Points that matter here:
  - 6-9: ask for no user data beyond what the app needs; collect, use or store user data only openly and with the user's permission.
  - 6-11 lists violations, including **(ز) "not providing responsive contact channels, such as email and phone number, inside the app and on the app's Bazaar page"** and (ه) not answering users' problems.
  - 6-14: don't use the «بازار» / «کافه بازار» marks without Bazaar's consent (the badge, §7, is the allowed way).
  - 6-19: every app needs a digital-content ID («شناسه الکترونیکی محتوای دیجیتال») from the Ministry of Culture; **Bazaar is authorised to obtain it for the developer**.
  - All official messages come from `@cafebazaar.ir` addresses, the docs, the panel or the developer blog; check spam.
  - Paid apps and in-app sales: developer gets **85%** of net sales per app per Iranian year up to 10 billion rials, **70%** above that.
- Support: tickets in the panel, `developers@cafebazaar.ir`, phone 021-91009798 (Sat–Wed working days, 8–18).

## 2. The app file

Sources: [package-name](https://developers.cafebazaar.ir/fa/guidelines/build-app/package-name) (2025-01-07), [manage-version](https://developers.cafebazaar.ir/fa/guidelines/build-app/manage-version), [app-signature](https://developers.cafebazaar.ir/fa/guidelines/build-app/app-signature), [common-errors](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/common-errors) (2024-11-04), [practical-principles](https://developers.cafebazaar.ir/fa/guidelines/getting-started/practical-principles), [app_bundle](https://developers.cafebazaar.ir/fa/guidelines/feature/app_bundle) (2025-01-15), [device-specification](https://developers.cafebazaar.ir/fa/guidelines/feature/device-specification), [processor architecture](https://developers.cafebazaar.ir/fa/guidelines/build-app/development-based-on-processor-architecture), [panel overview](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/overview).

- **Package name**: permanent once published; a new name is a new app. Allowed: `A–Z a–z 0–9 _`; each segment must start with a letter (no digit or `_` first); no `-`; Java keywords need a `_`; lowercase recommended. Must be unique on Bazaar. Must not contain banned or unrelated words (examples given: `ir.vpn.wallpaper`, `com.filtershekan...`) and must not copy another app's ID from Google Play.
- **Target SDK**: since Mehr 1403 (Sept 2024), **targetSdkVersion below 32 can't be published**. Panel error: `APK sdk version is below 32`. The guide also advises keeping target SDK at Google's latest. **No min SDK rule.**
- **Version code** must go up with every upload (any larger integer; +1 suggested). A rejected upload with no changes can be resubmitted with the same version. A deleted version's code can never be reused.
- **Signing**: release key only (debug-signed builds are refused); every update must use the same key as the last published package. Keep two backups of the keystore; validity at least 25 years (`-validity 10000` minimum on the command line).
- **Format**: «تنها بارگذاری فایل APK و AAB» are accepted (no ZIP/RAR). APK max **150 MB**; up to two OBB files of 2 GB each.
  - **AAB** needs Bazaar's offline **bundle signer** (`https://github.com/cafebazaar/bundle-signer/releases`): it outputs a `.bin` you upload next to the `.aab`; Bazaar never holds your key. `java -jar bundlesigner-<ver>.jar genbin --bundle app.aab --bin <dir> --v2-signing-enabled true --v3-signing-enabled false --key ... --cert ...` (or keystore flags). If you published APKs before, generate the bin with the same key. The launch checklist "recommends" AAB for smaller downloads.
  - Several APKs in one release («بارگذاری چند بسته در یک رهانش»): same package and key; one APK per (CPU, SDK) pair; CPUs `armeabi-v7a`, `arm64-v8a`, `All`.
- The first release of an app can't be deleted by the developer (only through support).

## 3. Upload steps and listing assets

Sources: [app-release](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/app-release), [panel overview](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/overview) (2025), [app-detail](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/app-detail), [app-name](https://developers.cafebazaar.ir/fa/guidelines/build-app/app-name), [description](https://developers.cafebazaar.ir/fa/guidelines/build-app/app-description-guideline), [category](https://developers.cafebazaar.ir/fa/guidelines/build-app/category-description), [icon-design](https://developers.cafebazaar.ir/fa/guidelines/build-app/icon-design) (2026-09-16), [launch-checklist](https://developers.cafebazaar.ir/fa/guidelines/getting-started/launch-checklist), [search](https://developers.cafebazaar.ir/fa/guidelines/app-growth/search).

Panel path: «برنامه‌ها» → «برنامه‌ی جدید», then seven steps:

1. **Upload APK/AAB.** The panel shows versionCode, versionName, TargetSDK, MinSDK, CPU.
2. **«اطلاعات پایه»**: developer title; type (app or game); category (one only); pricing («رایگان» or paid). **Pricing can't change after publishing**; paid + in-app purchase together isn't allowed; tick «برنامه نیاز به پرداخت درون‌برنامه‌ای دارد» for in-app billing. Paid price above 10,000 toman needs approval.
3. **«پرداخت درون‌برنامه‌ای»** (only if ticked): RSA key, JWT key (dynamic discount), products.
4. **«اطلاعات برنامه»**:
   - «نام فارسی برنامه»: max **100** characters; beyond **16** it's cut off in lists.
   - «توضیحات کوتاه فارسی»: max **60** characters, shown as the first one or two sentences.
   - «توضیحات فارسی»: the full text behind «بیشتر». The description guide says **800** characters.
   - English name and descriptions are optional.
5. **«اطلاعات پشتیبانی»**: phone, **support email (required)**, website (Persian and English). Use a support email different from the developer email.
6. **Images**:
   | Asset | Rule |
   |---|---|
   | Icon | PNG, 1:1, min **512×512**, max 3000×3000, max **3 MB**. Upload square, **no rounded corners, no outer shadow**: Bazaar rounds corners and adds its own shadow and background (since 1 Ordibehesht 1401). Leave padding if the shape touches the edges. No screenshots or photos in the icon, little or no text, no "NEW". **Must be the same as the launcher icon.** |
   | Screenshots | At least **1**, up to **12**, any size, max **1 MB** each; 5–6 good ones recommended. Must show the real app; no unrelated images. |
   | Header image («تصویر سر صفحه») | PNG, min **720×288**, ratio **5:2**, max 1 MB. Shown at the top of the app page. |
   | Promo screenshots («اسکرین‌شات‌های پروموشن») | JPG, min **1152×648**, **16:9**, max 1 MB. Used in automatic picture rows; suitable screenshots get copied here once automatically. |
   | Video | An **Aparat** link only (separate English video allowed); max 10 minutes. |
7. **Final check**: choose «انتشار دستی» (manual) or «انتشار خودکار» (auto). After «مرحله‌ی بعد» a popup lists common rejection reasons for similar apps. Then **«توضیحات بیشتر برای بررسی‌کننده»** (notes for the reviewer) with optional file uploads (permission letters, licences), and «درخواست انتشار برنامه».

Listing rules (from app-detail, app-name, deceptive-behavior, description):
- **The listing name must equal the name shown on the phone after install** (the label in the code). Same for the icon.
- No feature lists or keywords in the name; no "best", years, «جامع», «دخترانه»… Bad example from the docs: «منوی متنوع محلی، ایرانی و خارجی غذا»; good: «مامان‌پز».
- Description: short, a list of main features; no greetings, no user reviews, no requests for ratings with incentives, no other apps or stores, **no features planned for later updates** (tell users about those inside the app), no «تخفیف», «جایزه», «حراج». State limits (Android versions, phones it fails on, early-stage bugs). Listing text and images must suit age 4+.
- Search: content tags from name, description, images and video; changes reach search in about **6 hours**. Keyword stuffing is a violation.
- **Categories for apps** (the full list): آب و هوا، آشپزی و رستوران، آموزش، **ابزارها**، امور مالی، ورزشی، خرید، تناسب اندام، کتاب‌ها و مطبوعات، کودک، موسیقی، پزشکی، رفت و آمد، سبک زندگی، سرگرمی، سفر، شبکه‌های اجتماعی، شخصی‌سازی، عکاسی و ویدیو، مذهبی. **There is no «بهره‌وری».** «ابزارها» explicitly covers "reminders («یادآوری کننده‌ها»)… planning and productivity… task management apps and multi-purpose calendars". «سبک زندگی» is fashion, decor, etiquette and charity. A wrong category can be changed by Bazaar.
- Age rating: documented **for games only** (ESRA via the «اسرا» system, 3+/7+/12+/15+/18+, currently optional; [game-age-rating](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/game-age-rating), 2026-09-28). No age-rating step for apps is documented.
- The "Get it on Bazaar" badge: black background, unmodified, min 40 px online, always linked to the app page. Files: `https://s.cafebazaar.ir/2/images/badge-new.png` (fa), `.../get-cafebazaar-en.png` (en). ([application-badge](https://developers.cafebazaar.ir/fa/guidelines/learn-more/application-badge))

## 4. Review and release

Sources: [panel overview](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/overview), [app-release](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/app-release), [spam-apps](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/spam-apps), [unpublishing reasons](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/unpublishingreasons-forapps), [how-to-delete](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/how-to-delete-the-application), [launch-checklist](https://developers.cafebazaar.ir/fa/guidelines/getting-started/launch-checklist).

- Every app and every update is reviewed by a person. Result within **3 working days** (Thursday and Friday are not working days), by email and in the panel.
- After approval: manual or auto publish; going live can take **up to 4 hours** (server sync). A release left in «آماده‌ی انتشار» for **60 days** must be resubmitted.
- States: «بارگذاری‌شده» · «در صف انتظار» · «نیاز به تغییرات» · «ردشده» · «حذف‌شده» · «آماده‌ی انتشار» · «منتشرشده» · «تعلیق شده».
- New version: «رهانش‌ها» → «رهانش جدید» → upload → changelog in Persian (and English) → «ذخیره و انتشار». Only one non-final release at a time (deactivate a rejected one first).
- **Max 10 publish requests per day** per account (new apps plus updates).
- Most common rejection reasons (spring 1400): copyright, security/privacy, technical faults (crashes, broken parts), unnecessary permissions, wrong listing text.
- After publishing, an app whose function breaks is taken down.
- Launch checklist: test with "at least 50 people" before publishing; declare supported Android versions and screens.
- Deleting an app: by email or ticket, "درخواست حذف برنامه با Package Name «#» را دارم."; the package name is then burned forever.

## 5. Alpha release and staged rollout

Sources: [alpha-release](https://developers.cafebazaar.ir/fa/guidelines/feature/alpha-release) (2026-09-29), [staged-rollout](https://developers.cafebazaar.ir/fa/guidelines/feature/staged-rollout) (2025-01-15).

**«رهانش آلفا»** (alpha):
- Create a group: app → «رهانش‌ها» → «گروه‌های رهانش» → «گروه جدید»; add members **by mobile number**; **max 100 members per group**.
- New release → «گروه انتشار» = «آلفا» → pick the group. Alpha releases **also need a review request**.
- Testers must be logged in to Bazaar **with the same mobile number**; only they see and install it.
- A release can be switched from alpha to «عمومی» (public) while in the review queue or after publishing; never back to alpha.

**«رهانش تدریجی»** (staged rollout):
- Needs **at least 5,000 active installs**; not available for early releases.
- The percentage is each user's chance to get the update. It can be raised (no review needed), never lowered after publishing. 100% ends the rollout. Cancelling stops new users getting it; users who have it keep it.
- Only one staged rollout at a time; finish (100%) or cancel it before a new release (panel error otherwise).

## 6. Updates: Bazaar's update check, BazaarUpdater, in-app update

Sources: [update-check](https://developers.cafebazaar.ir/fa/guidelines/feature/update-check) (2022-02-23), [bazaarupdater-sdk](https://developers.cafebazaar.ir/fa/guidelines/feature/bazaarupdater-sdk) (2026-09-01), [intent](https://developers.cafebazaar.ir/fa/guidelines/feature/intent) (2025-01-19); library source `github.com/cafebazaar/BazaarUpdater` tag **1.2.0** (2026-10-16), read 2026-11-06.

**Documented AIDL service** ("بررسی به‌روزبودن برنامه"):
- Put `IUpdateCheckService.aidl` in package `com.farsitel.bazaar` (download: `http://s.cafebazaar.ir/2/resources/aidl/IUpdateCheckService.aidl`). That official file has **one method**:
  ```aidl
  package com.farsitel.bazaar;
  interface IUpdateCheckService {
      long getVersionCode(String packageName);
  }
  ```
- Bind: `Intent("com.farsitel.bazaar.service.UpdateCheckService.BIND").setPackage("com.farsitel.bazaar")`, `BIND_AUTO_CREATE`; `IUpdateCheckService.Stub.asInterface(binder)`; unbind when done.
- `getVersionCode(pkg)` returns **the Bazaar version code if it is newer than the installed one, otherwise −1**. It only answers for the caller's own package (anything else → −1). Bazaar caches, so a new release can take a while to show.

**BazaarUpdater library** (documented SDK):
- `maven { url 'https://jitpack.io' }` + `implementation("com.github.cafebazaar:bazaarUpdater:[1.1.2]")` (the doc's version; GitHub has 1.1.3 and 1.2.0). On 2026-11-06 JitPack returned **200** for `com/github/cafebazaar/bazaarUpdater/1.1.2/bazaarUpdater-1.1.2.pom`.
- Needs Android 5+ and Bazaar 26.2.0+ (for auto-update).
- `BazaarUpdater.getLastUpdateState(context) { result -> }` → `UpdateResult.AlreadyUpdated` · `UpdateResult.Error` (`result.getError()?.message`) · `UpdateResult.NeedUpdate` (`result.getTargetVersionCode()`).
- `BazaarUpdater.updateApplication(context)`: opens Bazaar's update sheet without leaving the app.
- `BazaarAutoUpdater.getLastAutoUpdateState(context) { r -> r.getState() }` → `AutoUpdateState.ENABLED / DISABLED / NOT_SUPPORTED`; `BazaarAutoUpdater.enableAutoUpdate(context)` shows Bazaar's screen to turn on daily auto-update for this app.
- **No forced update** ("در حال حاضر BazaarUpdater SDK از قابلیت به‌روزرسانی اجباری پشتیبانی نمی‌کند").
- Detection fails if the installed build's package name or signing key (keystore and alias) differs from the published one.

**What the library actually does** (source at 1.2.0, `updater/src/main`):
- Its AIDL has four methods in this order: `long getVersionCode(String)`, `long getRemoteVersionCode(String)`, `int getNewVersionState(String)`, `boolean prepareDownloadedUpdateInstall(String)`. The first two match the order of Lantern's copy, so binder codes 1 and 2 are right.
- It calls **`getRemoteVersionCode` only when Bazaar's own versionCode ≥ `2400700`** (Bazaar 24.7.0), else `getVersionCode`. −1 means error; a value ≤ the installed version means up to date.
- Constants: `BAZAAR_UPDATE_INTENT = "com.farsitel.bazaar.service.UpdateCheckService.BIND"`, `BAZAAR_AUTO_UPDATE_INTENT = "com.farsitel.bazaar.service.AutoUpdateCheckService.BIND"`, `bazaar://details/modal?id=`, `bazaar://autoupdate/modal?id=`, `bazaar://install/modal?id=` (+ `&prepared=1`), web fallback `https://cafebazaar.ir/app/?id=`. `getNewVersionState` needs Bazaar 30.1.0 (`3000100`); auto-update needs `2600200`.
- Its manifest adds only `<queries><package android:name="com.farsitel.bazaar" /></queries>`; no permissions.

**Rules on updates** ([security rule](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/security)): apps installed from Bazaar **may only be updated through Bazaar**; replacing or updating them by other means isn't allowed; developers must keep their latest version on Bazaar. All update versions are free for anyone who got the app once (contract supplement 1-4).

## 7. Intents and deep links

Source: [intent](https://developers.cafebazaar.ir/fa/guidelines/feature/intent) (2025-01-19). All use `intent.setPackage("com.farsitel.bazaar")`.

| Purpose | Action | URI |
|---|---|---|
| App page | `Intent.ACTION_VIEW` | `bazaar://details?id=<PACKAGE_NAME>` |
| **Rate / comment page** | **`Intent.ACTION_EDIT`** | **`bazaar://details?id=<PACKAGE_NAME>`** |
| Developer's apps | `ACTION_VIEW` | `bazaar://collection?slug=by_author&aid=<DEVELOPER_ID>` |
| Bazaar login | `ACTION_VIEW` | `bazaar://login` |
| In-app update sheet (newer Bazaar only) | `ACTION_VIEW` | `bazaar://details/modal?id=${context.packageName}` |

- Rating needs the user logged in to Bazaar; if they already rated, they get an edit screen.
- If Bazaar isn't installed the intent fails; check first (`getLaunchIntentForPackage("com.farsitel.bazaar")`) and fall back to the website.
- Login-state service (Bazaar 6.5+): AIDL `ILoginCheckService` (`http://s.cafebazaar.ir/2/resources/aidl/ILoginCheckService.aidl`), bind action `com.farsitel.bazaar.service.LoginCheckService.BIND`, `boolean isLoggedIn()`. ([login-check](https://developers.cafebazaar.ir/fa/guidelines/feature/login-check))

## 8. Ratings, reviews, replies

Sources: [deceptive-behavior](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/deceptive-behavior) (2022-09-25), [app-detail](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/app-detail), [response-user-comments](https://developers.cafebazaar.ir/fa/guidelines/app-growth/response-user-comments), [comments-api](https://developers.cafebazaar.ir/fa/guidelines/feature/comments-api) (2025-10-08).

- Asking for a review is allowed **if it doesn't push for a positive one**. Allowed example: «اگر از این بازی خوشتون اومده، در بازار برای ما نظر خودتون رو ثبت کنید». **Banned**: rewards for reviews (coins, prizes), repeated or forced asks, asking for "5 stars", fake reviews or installs.
- Replies to reviews: polite, on topic; **no links, emails, IDs, other channels**, no asking for personal data, no VPN talk, no ads. To move a user to support, tell them to use «اطلاعات توسعه‌دهنده» on the app page. Rule-breaking replies aren't shown and earn a warning.
- Comments API: base `https://sandoghche.cafebazaar.ir`, `GET /api/v0/reviews/` (filters like `publish_date_after`), reply endpoints; auth with the panel API token in a header.

## 9. Publishing rules

Index: [rules](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules). Publishing means accepting them; Bazaar needn't warn before acting. Rule-change log: [policy-updates](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/policy-updates) (last entry 1400/06/24).

**Content** ([content](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/content), 2022-12-21). "Content" includes database, code, package name, name, description, developer title, text, images, sound, ads, in-app products. Banned: anything against Iranian law (the article 749 criminal-content list), against "Islamic norms", insulting groups or religions, violent, "sensitive" (sexual themes, satanism, freemasonry, denying documented history), harmful or hidden functions, rooting tools. User-generated content needs moderation and a report button. **An update must not change the nature or purpose of the app.** Some apps need licences (video: Saramad and SATRA; scary games: an age rating from IRCG).

**Quality** ([quality](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/quality), 2022-06-21):
- Ask only for the minimum permissions for the core function; use permission data only for the core function.
- Keep and restore user state; no data loss on back or state changes.
- UI guidance: touch targets **44×44 pt**, text at least **11 pt**, enough contrast, no overlapping text, no distorted images.
- Apps that don't install or run, or whose main function is buggy, aren't published. Near-duplicates of your own or others' apps may be refused.
- **"Apps must do locally what can be done natively on the device, and not do it online or in a WebView without good reason."**

**Security** ([security](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/security), 2022-03-13):
- SDKs and libraries are part of your app; their data use is yours. WebViews of third-party sites need that site's permission, or open the browser.
- Use only permissions and APIs the current features need; rare exceptions need a convincing explanation with no alternative. Request at runtime where possible, explain why, **respect a refusal**, offer an alternative, don't pressure.
- SMS and Call Log groups only for default SMS/phone handlers; location only for location features, never for ads or analytics.
- Prefer intents for costly or sensitive actions (SMS, calls).
- Online payment goes through a Central-Bank-approved gateway **in the browser**, outside the app.
- **Apps must not mention other stores or distribution sites, or give access to apps from sources other than Bazaar.**
- Updates only through Bazaar (see §6). Don't force sign-up when it isn't core; ask for personal data only when the function needs it.

**Privacy** ([privacy](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/privacy), 2022-04-16; [privacy guide](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/privacypolicy)):
- Apps that record user data (phone, personal details…) must put a privacy text **inside the app**. With no sign-up, it goes somewhere easy to reach (the guide names an «درباره‌ما» section); with sign-up, it's shown at the first sign-up step, before collecting the number.
- The text says what's collected, why, how it's stored and protected, third parties, contact, cookies/trackers and how to opt out. **"If you collect no data, say so in the privacy policy."** Don't copy another app's policy.
- Sensitive data (IDs, financial, auth, contacts, email, location, SMS/calls, mic, camera): be transparent, limit use, encrypt in transit (HTTPS), never sell. Reading the installed-apps list needs consent and a mention in the policy.
- Prominent disclosure for unexpected collection (e.g. in the background): in the app, not only on a website, accepted by an explicit tap; back/home isn't consent.
- **No privacy-policy URL is required by any page read.** The panel's listing steps don't ask for one.

**Copyright** ([copyright rule](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/copyright), 2022-09-25; [copyright guide](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/copyright)):
- Iranian IP law applies. Citing sources is mandatory and isn't permission. **Iranian fonts need the creator's permission or licence terms.** Open-source licences must be followed. Photos, paintings and music are protected works.
- If the content is yours, **say so clearly inside the app**.
- **Using someone else's work needs a written confirmation sent to Bazaar.** It must contain **the developer email used on Bazaar and the app's package name**. From an organisation: sent from an address on its own domain, or on letterhead with date, stamp and the manager's signature. **From a private person: also their ID details, with images of their identity documents attached.** Forged letters → removal and action.
- Don't use other apps' names, icons or trademarks; unofficial versions must say «غیررسمی».

**Deceptive behaviour** ([deceptive-behavior](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/deceptive-behavior)): listing must match the app; name and icon on Bazaar must equal the installed name and icon; no hidden features; contests need written rules; no fake reviews or installs; no impersonation; the most fitting category.

**Ads** ([ads rule](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/ads)): ads only inside the app, never in widgets, notifications or keyboards; no ad shortcuts; no fake system UI. **Ongoing (sticky) notifications are not allowed unless the app needs one to work, and then the user should be able to dismiss or turn it off.** Paid apps with ads must say so in the description.

**Notifications** ([push_notification](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/push_notification)): no ads in notifications; no spam, no odd hours, no duplicates; same icon as the app; let users opt out of ongoing notifications.

**Payment** ([payment rule](https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/payment)): digital goods, subscriptions, unlockable features, "ad-free" versions and cloud/productivity software sold in the app **must use Bazaar's billing**, with no links to outside payment. Physical goods and services, banking and P2P must not. Prices in the app must match Bazaar's. Paid apps can become free later but not the reverse. Paid-app refunds: within 72 hours.

## 10. Permissions

Sources: [permission guide](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/permission) (2025-03-09), [android-access-guide](https://developers.cafebazaar.ir/fa/app-publish-guidelines/academy/android-access-guide) (2025-03-09), [panel overview](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/overview).

- **"The developer must have a clear explanation for every permission the app uses; otherwise the app won't be published."**
- Listed sensitive permissions and their allowed uses: `READ_CALENDAR`, `WRITE_CALENDAR` (prefer intent), `CAMERA` (prefer `ACTION_IMAGE_CAPTURE`), `READ_CONTACTS`/`WRITE_CONTACTS` (prefer pick/insert intents), `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `RECORD_AUDIO`, `CALL_PHONE` (prefer `ACTION_DIAL`), `ANSWER_PHONE_CALLS`, `READ_CALL_LOG`, `WRITE_CALL_LOG`, `SEND_SMS`, `RECEIVE_SMS` (prefer SMS Retriever), `READ_SMS`, `WRITE_SMS`, `KILL_BACKGROUND_PROCESSES`, `SYSTEM_ALERT_WINDOW` (not needed for notifications, widgets, toasts, PiP), `READ_HISTORY_BOOKMARKS`, `REQUEST_INSTALL_PACKAGES`, `CLEAR_APP_CACHE`, `RECORD_VIDEO`, `INSTALL_PACKAGES`, **`QUERY_ALL_PACKAGES` (not allowed; use `<queries>`)**.
- The panel names fine and coarse location as the "dangerous" permissions that need a reason in «توضیحات بیشتر برای بررسی‌کننده».
- **Not mentioned anywhere: `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `INTERNET`.** No rule on alarm or reminder apps and none on apps without internet.
- Library-added permissions can be stripped with `tools:node="remove"` (the access guide ends on this).
- Questions: `developers@cafebazaar.ir`, subject «راهنمای دسترسی‌ها».

## 11. Enforcement

Sources: [enforcement](https://developers.cafebazaar.ir/fa/app-publish-guidelines/enforcement), [policy-violation](https://developers.cafebazaar.ir/fa/app-publish-guidelines/enforcement/policy-violation), [app-suspension](https://developers.cafebazaar.ir/fa/app-publish-guidelines/enforcement/app-suspension) (2022-06).

- A violation takes the app down with an email naming the rule. Warnings («اخطار») give a few days to fix; ignoring one, or republishing the same problem, leads to removal or a ban. Bazaar isn't obliged to warn first.
- «تعلیق» (suspension): fix, wait out the days, resubmit; resubmitting unfixed can mean permanent removal. «حذف شده»: final. Account suspension: 30 days; repeat → permanent block (all apps down, new accounts blocked, no settlement).
- IP and trademark complaints go to `developers@cafebazaar.ir` in a fixed format.

## 12. Panel stats, install health, APIs

Sources: [statistics](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/statistics) (2025-02-09), [installation-health](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/installation-health), [cumulative-installation](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/cumulative-installation), [panel overview](https://developers.cafebazaar.ir/fa/guidelines/developers-panel/overview), [pishkhan-api](https://developers.cafebazaar.ir/fa/guidelines/feature/pishkhan-api) (2024-12-01).

- Dashboard: active installs, installs from Bazaar, daily revenue, ratings, **install health**. "Active install" = devices with Bazaar and your app that went online in the last two months. The app page shows cumulative unique installs from Bazaar (since Khordad 1401).
- **Install health** («سلامت نصب»): install-failure % over 20 days, per version, with error codes (`install_failed_update_incompatible`, `install_parse_failed_inconsistent_certificates` = key changed, `install_failed_version_downgrade`, `INSTALL_FAILED_INSUFFICIENT_STORAGE`…).
- **No crash or ANR report is documented** anywhere in the guide.
- Reviews: per-version, filter by date, version, Android version, stars; reply; report abusive ones. CSV export of stats; sales Excel up to 3 months.
- **Pishkhan API** (release automation): token per app from the app's «API پیشخان بازار» menu, header `CAFEBAZAAR-PISHKHAN-API-SECRET: <token>`, base `https://api.pishkhan.cafebazaar.ir/v1/`:
  - `POST apps/releases/` create release · `apps/releases/upload/` add APK (multipart `architecture=all|armeabi-v7a|arm64-v8a`, `apk=@file.apk`) · `upload-aab/`, `upload-bin/`, `bundle-status/` · `POST apps/releases/commit/` submit for review with `changelog_fa`, `changelog_en`, `developer_note`, `staged_rollout_percentage`, `auto_publish` · `last-committed/`, `last-uncommitted/`, `last-published/`; delete release.

## 13. In-app billing (Poolakey), for later

Sources: [in-app-billing intro](https://developers.cafebazaar.ir/fa/guidelines/in-app-billing/intro), [Poolakey Kotlin](https://developers.cafebazaar.ir/fa/guidelines/in-app-billing/implementation/kotlin), [security](https://developers.cafebazaar.ir/fa/guidelines/in-app-billing/security), [reference](https://developers.cafebazaar.ir/fa/guidelines/in-app-billing/reference), [faq](https://developers.cafebazaar.ir/fa/guidelines/in-app-billing/faq), [subscription](https://developers.cafebazaar.ir/fa/guidelines/in-app-billing/subscription), [dynamic-discount](https://developers.cafebazaar.ir/fa/guidelines/in-app-billing/dynamic-discount), [API](https://developers.cafebazaar.ir/fa/guidelines/in-app-billing/api).

- Setup: `maven { url 'https://jitpack.io' }`, `implementation "com.github.cafebazaar.Poolakey:poolakey:[latest_version]"`.
- `SecurityCheck.Enable(rsaPublicKey = "KEY")` (RSA key from step 3 of the upload) → `PaymentConfiguration(localSecurityCheck = …)` → `Payment(context, config)` → `payment.connect { … }` (disconnect when done) → `purchaseProduct(...)`, `subscribeProduct(...)`, `consumeProduct("PURCHASE_TOKEN") { … }`, `getPurchasedProducts { … }`.
- Product types: consumable and non-consumable (non-consumables are never consumed). Products are defined in the panel: SKU id, Persian/English title and description, price incl. tax, active flag, type (purchase or subscription).
- Server-side checks: the developer API (OAuth client from panel settings) validates purchases, subscriptions, refunds.
- Poolakey talks to the installed Bazaar app. **Whether it needs `INTERNET` isn't stated on the pages read**; check the library manifest before adopting it.

## 14. Money

Sources: [financial-settlement](https://developers.cafebazaar.ir/fa/guidelines/financial/financial-settlement-bazaar) (2025-07-07), [developers-share](https://developers.cafebazaar.ir/fa/guidelines/financial/developers-share), [shaparak fee](https://developers.cafebazaar.ir/fa/guidelines/financial/calculate-shaparak-fee), contract supplement.

- Share: 85/15 up to 10 billion rials per app per year, then 70/30.
- Settlement needs: verified account, a verified Sheba (IBAN) in the publisher's own name, «تصفیه‌حساب خودکار» ticked (set before the month ends), at least **50,000 toman** due (otherwise paid at the end of the year). Paid by the 5th working day of the next month; VAT by month end. Companies must register a net-sales invoice in the «سامانه مودیان» by the 21st.

## 15. Pages not covered in depth

All 116 guide and rules pages in the sidebar were fetched. These were only skimmed (not relevant to an offline, free to-do app): bazaar-login (in-app, web, Unity), expansion files (OBB), Trusted Web Application, Poolakey for Unity / React Native / Flutter / B4A / Java, the billing API sub-pages (setup old/new, validation, consume, subscriptions, refund, cancel, Postman), Snapp-Pay BNPL, Android TV billing, fast payment and subscription-revenue posts, ASO, UI/UX, uninstall prevention, featured-apps criteria, accessibility, Play Protect appeals, malware guide, foreign-developer sign-up, app transfer, SMS-code login, rename-package, "what's my package name", Studio signing walkthrough.

**Not read**: the «تبلیغات» (paid ads) section (`/fa/ads/...`: search ads, discovery ads, smart ads; 10 pages), and the developer blog. The panel itself (needs sign-in) wasn't opened.

---

## 16. What this means for Lantern

Checked against `CLAUDE.md`, `docs/BAZAAR-ROADMAP.md`, `docs/STORE-RELEASE-CHECKLIST.md`, D199, D207, D212–D215, `app/build.gradle.kts`, the merged manifest list in D212, and `app/src/bazaar/` on branch `bazaar` at `09e815c`.

### 16.1 Already met

| Item | Our state | Docs |
|---|---|---|
| Target SDK | 36 | ≥ 32 required (§2) |
| Min SDK | 26 | no rule |
| File format | signed APK from `scripts/release.sh` | APK accepted, ≤150 MB; AAB only "recommended" (§2) |
| Package name `app.lantern` | letters only, each segment starts with a letter | valid (§2); still must be free on Bazaar |
| Signing | one release key, same as beta APKs | updates need the same key (§2) |
| Version codes | 0.9.10 = 910; 1.0.0 = 10000 | must only go up (§2) |
| No `INTERNET`, works offline | yes | no rule against it; quality rules prefer local work (§9) |
| Sensitive permissions | none from Bazaar's list (`CAMERA` gone, D212) | §10 |
| Notifications | reminders only, no ads, no ongoing notification (no `setOngoing` in `app/src`) | §9 ads / notifications |
| Runtime requests | `POST_NOTIFICATIONS` asked at runtime (onboarding, `MainActivity`); the exact-alarm and battery screens open only when the user taps them in Settings' phone setup (`PhoneSetup.kt`) | "runtime where possible, respect refusal" (§9) |
| Privacy text in the app | «حریم خصوصی» in «درباره» (D215): nothing collected, no internet | in-app, easy to reach; "if you collect nothing, say so" (§9) |
| Open-source licences | «مجوزها» with full texts (D215) | "follow open-source licence terms" (§9) |
| Store links | `bazaar://details?id=app.lantern`, package `com.farsitel.bazaar`, then the website; no other store mentioned | §7, §9 security |
| **Rating intent** | `ACTION_EDIT` on `bazaar://details?id=app.lantern`, package `com.farsitel.bazaar` | exactly the documented intent (§7) |
| Rating ask | once, after a good streak, «الان نه» / «دیگه نپرس», no reward; copy «اگه دوستش داری، یه امتیاز تو بازار کمک می‌کنه…» | allowed form; no incentive, no repeat (§8) |
| Update check binding | `com.farsitel.bazaar.service.UpdateCheckService.BIND`, `setPackage("com.farsitel.bazaar")`, `BIND_AUTO_CREATE`, unbinds | documented (§6) |
| AIDL method order | `getVersionCode`, `getRemoteVersionCode` | same first two methods, same order as BazaarUpdater 1.2.0, so binder codes 1 and 2 are right (§6) |
| `<queries>` | `com.farsitel.bazaar` | same as the library's manifest; `QUERY_ALL_PACKAGES` avoided (§10) |
| Data never lost on update | migration and backup-sample tests (D213) | quality: keep user state (§9) |

### 16.2 Gaps and conflicts, with the fix

1. **Contact inside the app is required, not optional.** Contract 6-11(ز) lists "no responsive contact channels (email, phone) inside the app and on the app page" as a violation; step 5 makes a support email mandatory. Our contact row is hidden while `about_contact` is empty (D215). **Fix:** set `about_contact` before the first upload (an email is enough for the app; the panel needs the support email anyway), add it to `BazaarStringsTest`'s skip list, and use the same address as «ایمیل پشتیبانی». _Urgent: blocks B8._
2. **The listing name must equal the launcher label.** B8 plans the title «فانوس | لیست کارهای روزانه و یادآور»; the phone shows «فانوس». Docs: the panel name must match the installed name, and the name is no place for features or keywords. **Fix:** title «فانوس». Put the search words in «توضیحات کوتاه فارسی» (≤60 chars), e.g. «لیست کارهای روزانه با یادآور مطمئن، بدون اینترنت» (48 chars), and in the description (≤800 chars).
3. **There is no «بهره‌وری» category.** **Fix:** «ابزارها», which names reminders, task management and productivity. «سبک زندگی» is the wrong fit.
4. **Staged rollout isn't available to us.** It needs 5,000 active installs. B8's "use a staged rollout if the panel offers one" won't apply. **Fix:** use **alpha release** (§5) for the beta: up to 100 testers per group by mobile number, installed and updated through Bazaar itself, reviewed like any release, then switched to «عمومی». This also tests the real update check and rating intent, which a hand-sent APK can't (on a sideloaded install Bazaar has no listing to compare with).
5. **The crash/ANR panel isn't documented.** B8 says "read Bazaar's crash and ANR panel weekly"; the docs only describe install-failure health. **Fix:** change that line to "read install health and reviews weekly; confirm in the panel whether a crash view exists". Keep the local crash log (D211) as the main source.
6. **Age rating for apps isn't documented** (only ESRA for games, optional). **Fix:** drop "age rating" from B8's list, or mark it "only if the panel asks".
7. **Character art needs a permission letter in Bazaar's format.** «مجوزها» says "used with permission"; Bazaar wants the written confirmation **sent to them**, naming the developer email and `app.lantern`. If the artist is a private person: their ID details and copies of their ID documents too. If the art is ours, say so in the app. The per-character fonts: Bazaar names Iranian fonts as needing the creator's permission or licence terms. **Fix:** get the letter (or licences) per this format, upload it at step 7 with the reviewer notes, and make «مجوزها» name the artist/owner. _Blocks B8._
8. **Every permission needs a clear reason for the reviewer.** None of ours is on Bazaar's sensitive list, but the guide refuses apps whose permissions lack an explanation, and the store page shows the list to users. `ACCESS_NETWORK_STATE` in an app that says "no internet" may look odd. **Fix:** write «توضیحات بیشتر برای بررسی‌کننده» covering `USE_EXACT_ALARM` + `SCHEDULE_EXACT_ALARM` (reminders at the exact minute; the second is the fallback switch), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Xiaomi and others kill alarms otherwise; the system dialog opens only when the user taps it in Settings' phone setup), `RECEIVE_BOOT_COMPLETED` (re-arm alarms), `POST_NOTIFICATIONS`, `VIBRATE`, and WorkManager's `WAKE_LOCK`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE` (library defaults; the app has no `INTERNET` and sends nothing).
9. **Update check on old Bazaar versions.** We always call `getRemoteVersionCode` (binder code 2). The documented AIDL has only `getVersionCode`, and BazaarUpdater calls code 2 only on Bazaar ≥ 24.7.0 (`2400700`). On an older Bazaar our call either throws (caught → `null`) or returns 0 (filtered by `takeIf { it > 0 }`), so nothing breaks; the user just never hears about updates. **Fix (small, optional):** read Bazaar's versionCode; below `2400700` call `getVersionCode` (it returns the newer code or −1). Also note Bazaar caches, so «نسخه‌ی تازه اومده» can lag a release.
10. **Goals tab «در دست ساخت» is a review risk.** The quality rules refuse "incomplete apps" and the description may not mention future features (in-app mention is allowed). A reviewer may read an empty tab as unfinished. **Fix:** keep it out of the description and screenshots, and mention it in the reviewer notes ("planned section, clearly labelled; every other part is complete"). If the review objects, hide the tab in the `bazaar` flavor.
11. **Developer account details** (B8, you): individual account; national card photo; **the mobile must be in your own name**; the email is permanent (pick the one you'll keep); pay the 400,000-rial yearly subscription before publishing; developer title ≤32 chars; turn on two-step login.
12. **Listing assets** (B8) now have exact specs: icon 512×512 PNG ≤3 MB, square, no rounded corners or shadow (Bazaar adds both), same as the launcher icon; 1–12 screenshots ≤1 MB each; header image ≥720×288 at 5:2 PNG ≤1 MB (this is the "cover image"); optional promo images 1152×648 JPG; optional Aparat video ≤10 min. Screenshots must suit age 4+ (the vampire and goth art should be fine, but avoid anything dark-violent).
13. **Privacy-policy URL**: not required by Bazaar's docs or upload steps. **Fix:** keep it optional for Bazaar (it is still needed for Google Play); the in-app note satisfies Bazaar.

### 16.3 Changes to decisions

- **D199 ⑥ / B7 (beta):** prefer Bazaar's **alpha release** to hand-sent APKs. Same 3–5 testers, but through Bazaar, so the update and rating paths get tested too. Needs the account first, so B8's account step moves before B7.
- **B8 title and category:** «فانوس» as the listing name (it must match the launcher), category «ابزارها». Search words go in the short description.
- **B8 staged rollout:** not possible below 5,000 active installs; remove it. Use manual publish to control timing.
- **D215 contact row:** "hidden while empty" isn't acceptable for the Bazaar build; the address must be set for 1.0.
- **D214 reason is stale, choice still fine:** JitPack now serves `bazaarUpdater:1.1.2` (200 on 2026-11-06), so "JitPack 404" no longer holds. The direct AIDL call stays the better choice (no dependency, no `INTERNET`), but the library is now a working option, and it offers `bazaar://details/modal?id=` (update sheet without leaving the app) and auto-update (Bazaar 26.2.0+). Either could replace opening the plain details page from «بررسی به‌روزرسانی»; that's a small, separate change.
- **For later (plugins, D164/D165/D173):** Bazaar requires (a) paid digital content and unlockable features to use Bazaar billing, (b) no access to apps from sources other than Bazaar and updates only through Bazaar, and (c) no change in the app's nature after publishing. A plugin store that downloads code is close to (b) and (c). Raise this with Bazaar support before building `:store` for the Bazaar flavor.
