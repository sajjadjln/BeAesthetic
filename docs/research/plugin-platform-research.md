# Plugin platform: research report

_Written 2026-10-22 for the plugin platform task (`docs/tasks/plugin-platform.md`, D163–D176). Every source was read on 2026-10-22. The build plan based on this report is `docs/tasks/plugin-platform-plan.md`._

**Marks.** ✅ = I read the source today and it says this (most have a short quote). ⚠️ = partly verified (an old copy, a secondary source, or only part of the claim). ❓ = not verified: a lead, not a fact. `[S12]` points to the source list at the end.

---

## 0. The answer on one page

**Is a VS Code-style plugin platform achievable for this app?** Yes. Most of what makes VS Code extensions work maps to Android: a manifest, contribution points, lazy activation, an API version, string files per language, a separate UI context with theme variables, a signed marketplace. One part must not be copied. VS Code extensions are not sandboxed: "The extension host has the same permissions as VS Code itself" [S10]. Your rule "plugins must not read the user's other data" needs a real sandbox. The closer model is Figma: plugin logic in a sandboxed JavaScript engine, UI drawn somewhere else, messages in between [S24, S25].

**Can plugin code stay native (Kotlin/JVM bytecode)?** Only in two forms, and neither fits third-party plugins from your own store:
1. **Built into the APK.** A first-party plugin, like the workout, is just app code. Allowed on both stores.
2. **Each plugin as its own app, listed on Bazaar and on Play.** Allowed, but your store can't install it, every author needs store accounts, Google's developer verification applies to it from 2027 (outside sanctioned countries), and the plugin is a full app with its own permissions, network included.

Native code **downloaded from your store** is out on Google Play: "an app may not download executable code (such as dex, JAR, .so files) from a source other than Google Play" [S1]. A separate process doesn't change that. Bazaar has no written rule about downloaded code, but two of its written rules make it risky (§4.1). **Authors can still write Kotlin.** Kotlin compiled to JavaScript runs in an interpreter, which Play allows.

**D173 holds.** Play's exception covers "code that runs in a virtual machine or an interpreter where either provides indirect access to Android APIs" [S1]. It comes with a duty: interpreted code loaded at run time "must not allow potential violations of Google Play policies" [S1]. So for a Play build, your review checks plugins against Play's policies too. Bazaar has no written interpreter exception. Ask Bazaar in writing before the store ships in the Bazaar build.

**D166 needs one change.** Zipline's own README says it "does not offer a sandbox or process-isolation and should not be used to execute untrusted code" [S14]. A reviewed plugin by someone else is still untrusted code. So the engine must run in an Android isolated process: "a special process that is isolated from the rest of the system and has no permissions of its own" [S16]. Then even a bug in the engine can't reach your database, your files or the network.

**D172's level ② should change order.** A WebView gives the most freedom, but its rules (theme, direction, digits, no network) can only be checked by review and tests, and blocking its network takes several layers (§5). A host-drawn **free layout** (rows, columns, text, images, a canvas, gestures; colors only from your tokens; numbers and dates formatted by the host) gives most of that freedom and enforces the rules by code. Make that level ②. Keep the WebView as a later level ③, built only when a real plugin needs it.

**The one architecture I recommend:** signed script plugins; plugin logic in QuickJS inside an isolated-process service; a host API over Binder that reaches only the plugin's own goal, tasks and data; UI as trees that the host draws with your Compose components; contributions declared in the manifest, so most screens show without starting the plugin; plugins never run in the background; the workout as a built-in native plugin that implements the same contract. Reasons in §9. The build plan is `docs/tasks/plugin-platform-plan.md`.

---

## 1. What I used from the repo

| Fact | Where | Why it matters |
|---|---|---|
| `minSdk 26`, `targetSdk 36`, `compileSdk 36` | `app/build.gradle.kts` | Android 8 to 16. Some options need API 29 or 30 (§6.2). Targeting 36 turns predictive back on (§6.3). |
| Kotlin 2.3.20, AGP 8.13.2, Compose BOM 2026.01.01, Room 2.8.4, database version 10 | `gradle/libs.versions.toml`, `AppDatabase.kt` | Zipline's compiler plugin is tied to Kotlin versions (§4.2 d). |
| `MainActivity : ComponentActivity`; the locale is forced to `fa` by `PersianLocale.wrap`; `localeFilters += "fa"` | `MainActivity.kt`, `PersianLocale.kt`, build file | D174/D176 change these. Plugins read the app language from the same place (§6.4). |
| No `INTERNET`; a merged-manifest check and a banned-dependency check run in `check` | `app/build.gradle.kts` | D167 replaces the manifest check with a module check. |
| `USE_EXACT_ALARM`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `CAMERA` | `AndroidManifest.xml` | Not plugin questions, but they matter for a Play build (§8). |
| Workout code: `ui/workout/*`, `ui/progress/*`, `WorkoutRepository`, `ProgressRepository`, `PhotoStore` (about 3,900 lines) and the workout and check-in rules in `:domain` (about 750 lines). Tables: `exercise`, `workout`, `workout_exercise`, `session`, `set_log`, `round_number`, `metric`, `metric_entry`, `check_in`, `photo`, `milestone`. | `app/src/main/kotlin`, `domain/src/main/kotlin`, `AppDatabase.kt` | This becomes the first plugin. |

---

## 2. Q1: How VS Code extensions work, and what maps to Android

### 2.1 The parts

- **The extension host.** "The Extension Host is responsible for running extensions." There are three kinds: local Node.js, web (in the browser), and remote Node.js [S6] ✅. VS Code's aim is that "misbehaving extensions should not impact the user experience". The host stops extensions from "impacting startup performance", "slowing down UI operations" and "modifying the UI", and it loads them lazily [S6] ✅.
- **The manifest.** `package.json` with `engines` (required, and it "cannot be `*`"), `activationEvents`, `contributes`, `main` and `browser` entry points, `extensionKind` and `capabilities` [S7] ✅.
- **Contribution points.** Declarative JSON for commands, views, menus, settings and more [S7] ✅. VS Code shows them before the extension runs.
- **Activation events.** The extension loads only when needed: `onLanguage`, `onCommand`, `onView`, `onUri`, `onWebviewPanel`, `onStartupFinished`, `*` [S8] ✅. Since 1.74, contributed commands and views activate the extension without being listed. `*` is for when nothing else works [S8] ✅.
- **API versions.** `engines.vscode`, for example `^1.8.0`, so "the extension only gets installed for clients that contain the API you depend on" [S9] ✅.
- **Localization.** Manifest strings in `package.nls.json` and `package.nls.{locale}.json`; code strings through `vscode.l10n.t` (since 1.73). VS Code loads the file for the user's locale and falls back to English [S11] ✅.
- **Webviews.** "Think of a webview as an `iframe` within VS Code that your extension controls." It talks to the extension "using message passing". Theme colors arrive as CSS variables "prefixed with `vscode`", and the `body` gets a class for the current theme. A Content Security Policy is recommended and "JavaScript is disabled in webviews by default". VS Code warns: "Webviews are resource heavy" and "should also be used sparingly and only when VS Code's native API is inadequate" [S12] ✅.
- **Sandboxing.** Desktop extensions aren't sandboxed: "The extension host has the same permissions as VS Code itself" [S10] ✅. Web extensions run "restricted by the browser worker sandbox": no Node.js, "Creating child processes or running executables is not possible", network only through `fetch` with CORS [S13] ✅.
- **Marketplace.** "The Marketplace runs a malware scan on each extension package"; it stops name squatting; publishers can be verified "by proving domain ownership" (after 6 months on the Marketplace, with a domain at least 6 months old); "The Visual Studio Marketplace signs all extensions when they're published" and "VS Code checks this signature when you install an extension" [S9, S10] ✅.

### 2.2 What maps, what doesn't

| VS Code | In your app | Note |
|---|---|---|
| Extension host process | The plugin runtime in an isolated process | Stronger than VS Code: the process has no permissions. |
| `package.json` | `manifest.json` in the plugin package | Identity, version, API level, languages, permissions, contributions. |
| Contribution points | Extension points: goal template, goal-flow step, daily actions, Today card, goal sections, screens, chapter-review part, metrics, settings | Declarative, so the host can show entries without running plugin code. |
| Activation events | "Start the plugin when its screen opens or its Today card is stale" | Never in the background (§6.5). |
| `engines.vscode` | `minHostApi`, an integer level | Integer levels are simpler than version ranges for a single host. |
| `package.nls.*.json`, `l10n.t` | `strings/en.json`, `strings/fa.json`; text nodes carry keys and the host resolves them | The host also formats digits and dates, because QuickJS has no `Intl` (§4.2 d). |
| Webviews with theme variables | Level ②: host-drawn free layout with token colors. Level ③, later: a WebView with your design kit | VS Code itself says to use webviews sparingly. |
| Marketplace signing and the install check | The store signs approved packages; the app pins the store's public key | Same model. Add a revocation list, which Zipline lacks [S14]. |
| Verified publishers | Developer accounts you approve by hand | You review every version anyway. |
| **Not copied:** full Node.js access, extension-to-extension APIs, extension packs, remote hosts, keybindings, the command palette | | Each adds attack surface or UI with no use here. |

**The lesson.** Copy VS Code's declarations (manifest, contributions, activation, versions, string files). Don't copy its trust model. Figma rebuilt its plugin system after sandbox escapes in 2019. Its plugin code now runs in "QuickJS, a JavaScript VM written in C and cross-compiled to WebAssembly" [S25] ✅, and the plugin's UI runs inside an `<iframe>`, with the two parts talking "via message passing" [S24] ✅. Sandboxed logic plus separately drawn UI is the shape for this app.

---

## 3. Q2: Prior art on Android

| App | How plugins load | Isolation | Plugin UI | Languages | Store |
|---|---|---|---|---|---|
| **Tasker** | Separate apps using the Locale protocol: an edit Activity, a fire receiver, condition queries; settings travel in Bundles [S27] ✅ | OS (own app) | The plugin's own Activity | Its own resources | Each plugin is its own listing |
| **Muzei** | A separate app exports a `ContentProvider` (`MuzeiArtProvider`) guarded by `com.google.android.apps.muzei.api.ACCESS_PROVIDER`; Muzei queries it [S28] ✅ | OS | A settings Activity in the plugin | Its `label` and `description` resources | Each source is its own app |
| **Keyboards** | "an IME is an Android application that contains a special IME service", bound with `BIND_INPUT_METHOD` [S29] ✅ | OS | The IME draws its own input view | Its own resources | Own listing |
| **Home-screen widgets** | The host draws `RemoteViews`, which "doesn't support every kind of layout or view widget. You can't use custom views" [S30] ✅ | OS | Host-drawn, limited set | The widget app's resources | Own listing |
| **Termux add-ons** | Separate APKs (Termux:API, Termux:Widget and others). Android 10 made running files from an app's writable home directory "a W^X violation"; Termux stopped updating on Play [S31] ✅ | OS | Own | Own | F-Droid |
| **Mihon** (manga reader) | Extensions are APKs. Mihon checks the extension library version (1.4 or 1.6), checks the signing certificate against trusted stores, then loads the code **into its own process** with a class loader [S32] ✅ | None after loading; trust is by signature | Host-drawn from data the extension returns | Host | Not on Play ❓ |
| **Obsidian** | JavaScript plugins inside the app. "Obsidian cannot reliably restrict plugins to specific permissions or access levels." Plugins "can access files", "connect to internet", "install additional programs". Restricted Mode is on by default; every version is scanned; popular or flagged plugins get manual review [S33] ✅ | None beyond Restricted Mode | The plugin's own DOM | Its own | On Play ❓ |
| **Joplin** (mobile) | Plugins run inside a WebView (react-native-webview). From contributors' forum posts, not official docs [S34] ⚠️ | WebView | WebView | — | On Play ❓ |
| **Firefox** for Android | WebExtensions in the browser engine. "Extensions and themes need to be signed by Mozilla before they can be installed in release and beta versions of Firefox"; automated validation, manual review at any time [S35] ✅ | Browser engine | Extension pages | ❓ | On Play |
| **Kiwi Browser** | Chrome extensions on Android. Discontinued in January 2025; its extension code moved into Microsoft Edge Canary [S36] ⚠️ (news) | — | — | — | Removed from Play |
| **Android Auto (Car App Library)** | Apps fill "a set of templates designed to meet driver distraction standards", and must "meet specific design requirements before it can be listed on Google Play" [S37] ✅ | — | Templates | App resources | Play, with design review |
| **Cash App Zipline + Redwood** | Kotlin/JS in QuickJS; UI described as a tree that native widgets draw. Redwood: "This project is no longer under active development" [S15] ✅ | In-process, and Zipline says it isn't a sandbox [S14] | Host-drawn tree | Host | ❓ (current production use not checked) |

**Three families.**
1. **Plugin = a separate app** (Tasker, Muzei, keyboards, Termux). The OS isolates it. It draws and translates itself. Each plugin is its own store listing. The host loses control of review, design and data.
2. **Plugin = code loaded into the host** (Mihon). No isolation once loaded. Only seen outside Play.
3. **Plugin = script in the host** (Obsidian, Joplin, Firefox, Figma). Works on Play under the interpreter exception. The host decides how strong the sandbox is. Obsidian chose none; Firefox and Figma chose a real one.

Android Auto is the best Android proof for level ①: third-party content shown through host templates, so the design rules hold by construction.

---

## 4. Q3: Every way to run plugin code

### 4.1 The store policies, quoted

**Google Play, Device and Network Abuse** [S1] ✅:
> "An app distributed via Google Play may not modify, replace, or update itself using any method other than Google Play's update mechanism. Likewise, an app may not download executable code (such as dex, JAR, .so files) from a source other than Google Play. This restriction does not apply to code that runs in a virtual machine or an interpreter where either provides indirect access to Android APIs (such as JavaScript in a webview or browser)."

> "Apps or third-party code, like SDKs, with interpreted languages (JavaScript, Python, Lua, etc.) loaded at run time (for example, not packaged with the app) must not allow potential violations of Google Play policies."

Its list of violations includes "Apps that install other apps on a device without the user's prior consent" and "Apps or third party code (for example, SDKs) containing a webview with added JavaScript Interface that loads untrusted web content (for example, http:// URL) or unverified URLs obtained from untrusted sources (for example, URLs obtained with untrusted Intents)."

**Google Play, `REQUEST_INSTALL_PACKAGES`** [S2] ✅: allowed only when the core functionality includes "Sending or receiving app packages, AND" "Enabling user-initiated installation of app packages", and the use is one of: web browsing or search, communication with attachments, file sharing or management, enterprise device management, backup and restore, device migration. A to-do app's plugin store isn't on the list.

**Android developer verification (Google, certified phones)** [S3, S4, S5] ✅: enforcement starts 30 September 2026 in Brazil, Indonesia, Singapore and Thailand, for installs from participating stores (Google Play, Galaxy Store, Xiaomi GetApps and others), and expands "globally for all apps on certified Android devices in 2027". Bazaar isn't in the first list. On sanctions: "developers located in these regions won't be able to complete Android developer verification. However, the Android developer verification checks won't run on certified Android devices physically located in comprehensively sanctioned countries." ADB installs stay exempt. This covers APKs, not script plugins.

**Cafe Bazaar, security rules** [S19] ⚠️. The live site returns 403 outside Iran. This is the copy archived on 2024-04-18 (page last updated ۱۴۰۰-۱۲-۲۲). The rules history lists no change after ۱۴۰۰/۰۶/۲۴ [S20].

> «برنامه‌ها نباید در محتوای خود به سایر فروشگاه‌ها و سایت‌های توزیع برنامه یا بازی اشاره کنند یا امکان دسترسی به برنامه‌ها را از منابعی به غیر از بازار فراهم کنند.»

("Apps must not mention other stores or app and game distribution sites, or give access to apps from sources other than Bazaar.")

> «برنامه‌هایی که از بازار دانلود می‌شوند تنها باید از طریق بازار به‌روزرسانی شوند. جایگزینی، تغییر و به‌روزرسانی برنامه از روش‌های دیگر مجاز نیست.»

("Apps downloaded from Bazaar must be updated only through Bazaar. Replacing, changing or updating the app by other methods is not allowed.")

> «اگر برنامه کارکرد پیچیده‌ای داشته باشد؛ از کاربر دسترسی‌های نالازم بخواهد؛ یا به اطلاعات حساسی از کاربر دسترسی داشته باشد، از سوی بازار برای انتشار پذیرفته نخواهد شد.»

("If an app has opaque functionality, asks for unneeded permissions, or reaches sensitive user data, Bazaar won't publish it.")

The same page has two WebView notes [S19] ⚠️. A site not affiliated with the app should open in the browser, or the app needs that site's permission to show it in a WebView. And if a WebView reaches sensitive data, «بهترین روش برای در امن نگه داشتن این داده‌ها، پاک کردن فایل‌های ذخیره شده محلی (Local) است» ("the best way to keep that data safe is to clear the locally stored files"), with `clearCache()` or a `no-cache` header. The store pages are your own site, so the first note is met; clearing the cache on exit follows the second.

Bazaar's payment rules say features and digital content must be sold only through Bazaar's own billing [S21] ✅ (archived July 2024). Not an issue while plugins are free (D169). Interesting detail: the same page lists "updating some parts of the app" («بروزرسانی بعضی از بخش‌های برنامه») among things an app may sell, so downloadable add-on content isn't foreign to Bazaar's rules.

**Not found:** any Bazaar rule about downloading or interpreting code. Bazaar has neither a ban nor an exception in writing ❓.

### 4.2 The options

#### a. A separate APK per plugin, with an SDK (AIDL/Binder, ContentProvider, bound services, Activities)
- **Security and isolation:** the strongest default. The OS runs the plugin as another app with its own user ID and files. But it is an app: it can hold `INTERNET` and send out whatever you give it. You can refuse to bind to a plugin whose `PackageInfo` asks for `INTERNET`, but it can still open a browser with data in the URL. (Design reasoning, not a source.)
- **Native Kotlin:** yes.
- **UI:** its own Activities (rules by review only), `RemoteViews` (limited [S30]), or `SurfaceControlViewHost` (API 30+, §5).
- **Languages and direction:** the plugin's own resources. Following *your app's* language rather than the phone's needs your SDK to pass it and the plugin to apply it. Hard to guarantee.
- **Data and backup:** the data lives in the plugin's app. Your backup can't include it unless it goes through your API.
- **Performance and size:** a process per plugin; cross-process calls; one APK per plugin.
- **Discovery:** apps targeting Android 11+ see other apps filtered "by default"; you need `<queries>` [S17] ✅.
- **Developer experience:** a normal Android project, plus AIDL versioning.
- **Review and signing:** the stores list each plugin. You can pin allowed signing certificates, as Mihon does [S32].
- **Maintenance:** high; per-plugin apps, per-plugin store listings.
- **Google Play:** allowed if each plugin is its own Play app. Installing plugin APKs from your store needs `REQUEST_INSTALL_PACKAGES`, which doesn't fit the allowed uses [S2]. From 2027, each plugin's author needs Google verification for installs outside sanctioned countries [S4].
- **Cafe Bazaar:** allowed if each plugin is its own Bazaar app. Offering APKs from your store breaks «امکان دسترسی به برنامه‌ها را از منابعی به غیر از بازار فراهم کنند» [S19].
- **Verdict:** possible, but you lose your store, your design rules and your backup rule.

#### b. Native code loaded into the app process (DexClassLoader / InMemoryDexClassLoader)
- **Security and isolation:** none. Loaded code has every right the app has: the database, the photos, `INTERNET`. Reflection reaches anything.
- **Platform:** `InMemoryDexClassLoader` exists from API 26 [S18] ✅. Apps targeting 34+ must mark every dynamically loaded file read-only, "Otherwise, the system throws an exception", and Google recommends that "apps avoid dynamically loading code whenever possible" [S22] ✅.
- **Native Kotlin:** yes. **UI:** anything. **Data:** anything, which is the problem.
- **Google Play:** banned when downloaded ("dex, JAR, .so") [S1].
- **Cafe Bazaar:** no written rule. «جایگزینی، تغییر و به‌روزرسانی برنامه از روش‌های دیگر» is a likely reading ❓.
- **Verdict:** no, for any code you didn't compile into the APK.

#### c. Native code in an isolated process (`android:isolatedProcess`, Binder contract)
- **Security and isolation:** strong. The service runs "under a special process that is isolated from the rest of the system and has no permissions of its own. The only communication with it is through the Service API" [S16] ✅. No network, no app files.
- **Platform details** ✅ [S23, S38]: `bindIsolatedService` (API 29) starts several instances of one service, one per name: one process per plugin. `useAppZygote` spawns isolated services from an app zygote to "reduce the service launch time and reduce its memory usage"; "the absolute limit is 100, but … a much safer limit to use in practice is 50". `BIND_SHARED_ISOLATED_PROCESS` (API 34) puts several isolated services in one process. `BIND_EXTERNAL_SERVICE` (API 24) runs another package's isolated service under the caller's package.
- **Cost:** a new process per plugin: start time and memory. No official numbers ❓. Measure on the Poco.
- **Google Play and Bazaar:** if the native code is **downloaded**, option b's answer applies; the process doesn't matter. If the code ships **inside the APK**, it's fine on both.
- **Verdict:** the right **sandbox**, for an interpreter (option d), not for downloaded native code.

#### c′. Native plugin code compiled into the APK and run in an isolated process
Curated plugins, reviewed, built into your app and shipped with app updates. Allowed on both stores. A plugin update waits for an app update. Maybe a later "built-in" tier; not a store.

#### d. A script runtime

| Engine | What it is | Sandbox | Status | Fit |
|---|---|---|---|---|
| **Zipline (QuickJS), in the app process** | Kotlin/JS precompiled to QuickJS bytecode; Kotlin interfaces across the bridge; "EdDSA Ed25519 and ECDSA P-256 signatures"; modules "can also be embedded with the host application to avoid any downloads if the network is unreachable" [S14] ✅ | "It does not offer a sandbox or process-isolation and should not be used to execute untrusted code" [S14] ✅ | 1.28.0 on 2026-09-29 (Kotlin 2.4.20, Android API 23+); 1.27.0 targets Kotlin 2.3.20 [S14] ✅ | A good engine; not safe alone. |
| **Zipline (QuickJS) inside your own isolated process** | The same engine and bridge, run inside option c. Plugin code is loaded as verified JavaScript with the public `loadJsModule(script, id)`; services are bridged with `bind` and `take` [S14] ✅. Zipline's `QuickJs` class alone can't call back into Kotlin: its call channels are `internal` [S14] ✅ | OS sandbox plus engine limits: `memoryLimit`, `maxStackSize`, and an `interruptHandler` that is "called back frequently during code execution" and returns true "to halt execution" [S14] ✅ | Zipline as above | **Recommended default.** |
| **quickjs-kt** (another QuickJS binding) | Public host functions (`defineBinding`), coroutines, timeouts by cancellation, `memoryLimit`, `maxStackSize`, bytecode `compile` [S58] ✅ | Same as above when run in an isolated process | 1.0.15 on 2026-09-03; one main maintainer [S58] ✅ | Fallback binding if Zipline's Kotlin coupling hurts. Needs your own JSON bridge. |
| **JavaScriptSandbox** (`androidx.javascriptengine`) | V8 from the phone's WebView, without a WebView. "All JavaScript is executed in a separate sandboxed process"; "Code that is run in a sandbox does not have any access to data belonging to the original app unless explicitly passed into it" [S26, S40] ✅ | WebView's `JsSandboxService0` is declared `isolatedProcess="true"` [S41] ✅; heap limits; crash callbacks; message ports since 1.1.0 [S26, S42] ✅ | 1.0.0 stable 2025-07-02; 1.1.1 stable 2026-09-23 [S42] ✅. Needs API 26 **and** WebView 104.0.5112.0 or newer; features differ by WebView version; one sandbox per app [S40] ✅ | **Plan B.** Depends on the WebView version, which many phones in Iran may not update. |
| **Hermes** | "A JavaScript engine optimized for running React Native" [S39] ✅ | No standalone Android API found ❓ | Released with React Native | No. |
| **J2V8 / Javet** | V8 bindings for Java | In the app process | J2V8 has no GitHub releases and was last pushed 2025-11; Javet 6.0.2 on 2026-10-04 [S39] ✅ | Large native libraries, no sandbox. No. |
| **Kotlin/Wasm** | Kotlin compiled to WebAssembly; Beta; needs WebAssembly garbage collection and exception handling [S43] ✅. Chasm, a WebAssembly runtime in Kotlin Multiplatform, supports Wasm 3.0 with GC and exceptions [S39] ✅ | An interpreter in the app process | Young | Watch, not now. Kotlin/Wasm on Chasm on Android isn't proven ❓ |

For the recommended option (QuickJS in an isolated process):
- **Native Kotlin:** the language, yes (Kotlin/JS). The bytecode, no.
- **UI:** a tree that the host draws (levels ① and ②), or a WebView (level ③, later).
- **Languages and direction:** owned by the host. QuickJS has no `Intl`: neither Bellard's QuickJS nor QuickJS-ng defines one (checked in their source) [S44] ✅. So the host formats every digit and date. That's what you want anyway.
- **Data and backup:** only through the host API, into your Room database, so it's in every backup by construction.
- **Performance:** QuickJS is an interpreter. Plugin logic here is small (lists, sums, UI trees). Start time and call latency must be measured on the Poco ❓.
- **Size:** QuickJS native libraries per ABI; measure ❓.
- **Developer experience:** Kotlin with your SDK, or TypeScript. A JVM test harness is possible because Zipline also runs on the JVM [S14] ✅.
- **Versioning:** an integer `hostApi` level; additive changes.
- **Review and signing:** you read the source; the store signs the package.
- **Maintenance:** Zipline "uses unstable APIs in its implementation and is sensitive to version updates", because its compiler plugin is tied to the Kotlin version [S14] ✅. The runtime module needs that plugin for `bind` and `take`, so the app's Kotlin upgrades wait for a Zipline release. Its changelog shows Kotlin upgrades shipping within weeks (2.3.20 in 1.27.0, 2.4.20 in 1.28.0) [S14] ✅. Skip `zipline-loader`: it pulls in OkHttp and an SQL cache [S14] ✅, and the host loads verified files itself. If the coupling ever hurts, quickjs-kt [S58] can replace Zipline behind the same contract, with a JSON bridge.
- **Google Play:** allowed under the interpreter exception; review against Play's policies [S1].
- **Cafe Bazaar:** no written rule either way; ask ❓.

#### e. Purely declarative plugins (JSON or a DSL, no code)
- **Isolation:** total; nothing runs.
- **Freedom:** low: templates, fields, simple formulas. It can't run a workout session.
- **Languages:** host-drawn, strings in files.
- **Stores:** it's data, not code. The safest.
- **Verdict:** the right base layer (the manifest's contributions are declarative), not enough alone.

### 4.3 Comparison matrix

| | a. Separate APK | b. DCL in the app process | c. Downloaded native code, isolated process | d. Script in an isolated process | e. Declarative |
|---|---|---|---|---|---|
| Isolation from user data | Strong (OS), but it has its own network | None | Strong | Strong (OS + engine limits) | Total |
| Stays native Kotlin | Yes | Yes | Yes | Language yes, bytecode no | No code |
| UI freedom | Full (own Activities) | Full | Via SurfaceControlViewHost (API 30+) | Levels ① ②, ③ later | Templates only |
| Follows the app's language and direction | Only if the plugin cooperates | Yes | Partly | Yes, by construction | Yes |
| Data in your backup | No | Yes | Through the API | Yes | Yes |
| Cost on a mid-range phone | A process per plugin | Lowest | A process per plugin | A process per plugin + an interpreter | None |
| APK size | Separate APKs | Small | Small | + the engine (measure) | None |
| Developer experience | Plain Android | Plain Android | Android + Binder | Kotlin/JS or TS + SDK | JSON |
| Versioning | AIDL versions | Binary compatibility pain | AIDL | `hostApi` level | Schema version |
| Your review and signing | Lost to the stores | Possible | Possible | Yes | Yes |
| Maintenance | High | Medium | High | Medium | Low |
| **Google Play** | Only as separate Play apps [S1, S2] | **Banned** if downloaded [S1] | **Banned** if downloaded [S1] | **Allowed** (interpreter) [S1] | Allowed |
| **Cafe Bazaar** | Only as separate Bazaar apps [S19] | Risky: no rule, but «تغییر برنامه» ❓ | Risky ❓ | Likely allowed; ask in writing ❓ | Allowed |

---

## 5. Q4: Plugin UI options

| Option | Freedom for the author | Two languages, both directions | How the minimum standard is enforced | Store risk |
|---|---|---|---|---|
| **Native Compose inside the app** | Full | Same as the app | Your own code review. Built-in plugins only | None (app code) |
| **An Activity from a plugin APK** | Full | The plugin's job; it must copy your app's language, not the phone's | Review only | Needs option a |
| **SurfaceControlViewHost** | Full. A view hierarchy from one process is embedded into another's `SurfaceView` [S45] ✅ | The plugin's process must apply your locale and theme | Review and screenshots | Needs native plugin code (a or c′). API 30+; your minSdk is 26 [S45] ✅ |
| **Host-drawn tree, level ①: your components** (tile, card, band, stepper, counter, chart, sheet…) | Low to medium | By construction: the host lays out with start/end, formats numbers and dates, resolves string keys | **By code.** No color, font or raw-number inputs exist | None |
| **Host-drawn tree, level ②: free layout** (rows, columns, boxes, scroll, text, images, canvas paths, gestures, animation) | High: any layout, custom drawing | By construction, as above; canvas drawings flip in RTL unless marked otherwise | **By code** for colors (tokens only), digits, dates, back and touch-target minimums. **Harness tests** for overflow, contrast with transparency, labels. **Review** for the product rules | None |
| **RemoteViews / Glance** | Low; no custom views [S30] | App resources | Code | Built for widgets, not screens |
| **Sandboxed WebView with your design kit** (level ③, like VS Code) | Highest | You inject `lang`, `dir`, tokens as CSS variables and Estedad. With targetSdk 33+, "WebView always sets `prefers-color-scheme` according to `isLightTheme`" [S46] ✅ | **Lint and tests** (CSS colors, logical properties, an accessibility scanner such as axe-core) and **review**. Network blocking needs layers (below) | Play: no `addJavascriptInterface` with untrusted content [S1]. Use `addWebMessageListener`, which injects the bridge only into frames "whose origin matches `allowedOriginRules`" [S47] ✅ |

**Why a plugin WebView is harder than it looks.** The app will hold `INTERNET` (D164), and a WebView's requests go out under your app's permissions. `setBlockNetworkLoads(true)` means "the WebView should not load resources from the network" [S48] ✅. In Chromium it makes loads cache-only (`LOAD_ONLY_FROM_CACHE`) [S49] ✅. That covers pages, `fetch` and XHR, which go through the URL loader. I couldn't confirm that it covers WebSockets or WebRTC ❓. So a plugin WebView needs all of these: `blockNetworkLoads`; `shouldInterceptRequest` that serves only plugin files through `WebViewAssetLoader` and refuses everything else; a CSP with `connect-src 'none'` (it covers fetch, XHR and WebSocket); `RTCPeerConnection` removed by a document-start script; no file or content access (file access is already off by default when targeting API 30+ [S48] ✅); no navigation away; and a review that rejects network code. It can be done, but each layer is a place to slip.

---

## 6. Q5: Platform limits

### 6.1 Dynamic code loading
- **Android 14, apps targeting 34+:** "all dynamically-loaded files must be marked as read-only. Otherwise, the system throws an exception" [S22] ✅. This is about DEX, JAR and APK files loaded by the platform. A JavaScript file read by your interpreter isn't loaded by the platform, so the rule doesn't apply to it. Still, make plugin files read-only after install, as hygiene.
- **Android 10:** "Execution of files from the writable app home directory is a W^X violation" [S31] ✅. An interpreter inside your APK is fine.

### 6.2 What each option needs

| Need | API level | Source |
|---|---|---|
| `InMemoryDexClassLoader` | 26 | [S18] ✅ |
| `isolatedProcess` service | 1 | [S16] ✅ |
| `BIND_EXTERNAL_SERVICE` | 24 | [S38] ✅ |
| `bindIsolatedService` (one process per plugin) | 29 | [S38] ✅ |
| `useAppZygote`, `zygotePreloadName` | 29 ❓ (level from memory; the behavior is from [S23] ✅) | [S23] |
| `BIND_SHARED_ISOLATED_PROCESS` | 34 | [S38] ✅ |
| `SurfaceControlViewHost` | 30 | [S45] ✅ |
| Package visibility filtering (`<queries>`) | targetSdk 30+ | [S17] ✅ |
| JavaScriptSandbox | 26, and WebView 104.0.5112.0+ | [S40] ✅ |
| WebView content in "a separate, isolated process" | 26 (Android 8.0) | [S50] ✅ |
| `Signature`: `SHA256withECDSA` / `Ed25519` | 11+ / 33+ | [S51] ✅ |
| `Process.isIsolated()` (so `App.onCreate` can skip work in the plugin process) | 28 | [S60] ✅ |

With minSdk 26: one isolated process per plugin works from Android 10. On Android 8 and 9, run all plugins in one isolated process, each with its own QuickJS runtime.

### 6.3 Android 16 (you target 36)
- For apps targeting 36 on Android 16, "`onBackPressed` is not called and `KeyEvent.KEYCODE_BACK` is not dispatched anymore" [S52] ✅. Plugin back handling must go through the host's back dispatcher (Compose `BackHandler`), and the host forwards a back event to the plugin.
- The edge-to-edge opt-out is gone, and on screens 600 dp and wider, orientation and resizability limits are ignored [S52] ✅. Plugin layouts must handle insets and wide screens; host-drawn trees get this for free.

### 6.4 Per-app language (D176)
- Android 13 brings per-app language in system settings and `LocaleManager` [S53] ✅.
- Older phones use the AppCompat 1.6+ backport. "If you're using Compose with `setApplicationLocales`, you must extend your activity from `AppCompatActivity`." `autoStoreLocales` stores the choice [S53] ✅. Your `MainActivity` is a `ComponentActivity`. So either switch to `AppCompatActivity`, or keep your own stored language and wrap the context below API 33, as `PersianLocale` does now.
- `generateLocaleConfig = true` (AGP 8.1+) with `res/resources.properties` builds the language list for the system [S53] ✅. The current `localeFilters += "fa"` must include `"en"`.
- **For plugins:** the host is the only source of "current language, direction, digits and calendar order". Plugins never read the phone's locale.

### 6.5 HyperOS and MIUI
- I found no official Xiaomi documentation ❓. Community reports say MIUI kills background work unless the user turns on autostart, sets battery to "No restrictions" and locks the app in recents [S54] ⚠️ (MIUI 10–14; HyperOS isn't mentioned).
- **Design answer:** plugins never run in the background. Reminders stay host tasks, on the path you already made reliable. A plugin process is bound only while a plugin screen is visible or a card refreshes in the foreground. If HyperOS kills it, the next use restarts it. The spike tests this on the Poco.

### 6.6 Android developer verification
Not a plugin question, but it touches distribution. From 2027, apps installed on certified phones outside sanctioned countries need a verified developer [S4] ✅. Developers in sanctioned countries can't verify, and the checks don't run on phones physically in those countries [S5] ✅. So the Bazaar build is unaffected inside Iran. Users abroad would need the "advanced flow" (a one-day wait, then "Install Anyway") [S5] ✅. Script plugins aren't APKs, so they aren't covered.

---

## 7. Q6: Security

**Code signing, with the store key pinned in the app.**
- Use **ECDSA P-256 with SHA-256**. Android has `SHA256withECDSA` from API 11; `Ed25519` only from API 33 [S51] ✅. .NET signs ECDSA natively. Its `DSASignatureFormat` has `IeeeP1363FixedFieldConcatenation` and `Rfc3279DerSequence` [S55] ✅. Without a format argument, .NET's `SignData` "will use IeeeP1363FixedFieldConcatenation"; the overload that takes a format exists from .NET 5 [S59] ✅. Android's `SHA256withECDSA` expects the DER form (standard Java behavior ❓, not re-read), so sign with `Rfc3279DerSequence` and confirm with a test vector in the spike.
- Sign a list of file hashes (each file and its SHA-256), not the zip bytes. Refuse a package with files that aren't on the list.
- Pin two public keys (current and next) so the key can rotate. Keep the private key off the web server (brief, P5).
- Add a signed revocation list. Zipline "does not yet provide a mechanism to outlaw older(signed) versions" [S14] ✅.
- Debug builds also accept a developer key for sideloading. Release builds never do.

**A per-plugin permission model.** Permissions are declared in the manifest, shown in the install sheet in the app's language, and enforced by the host. Capability style: a plugin gets only the API objects it was granted, and every call is scoped to its own goal, its own tasks and its own documents. The host knows which plugin sits on each Binder connection, so a plugin can't claim another plugin's identity.

**Preventing data exfiltration.** The isolated process has no network (no permissions [S16]). The host API has no network, intents, sharing, clipboard or file calls. Plugin text reaches only the screen, notifications (through host tasks) and the backup. What's left: a plugin could show misleading text; review covers that.

**Resource limits.** A QuickJS memory limit and an interrupt handler that stops a call over its time budget [S14]; a timeout on each Binder call; storage quotas per plugin; a cap on the tasks a plugin can create; write rate limits.

**Crash isolation.** A crash kills only the plugin's process. (For JavaScriptSandbox the docs say "the application's main process will be unaffected" [S26] ✅.) The host shows a calm card; after 3 crashes in a day the plugin pauses until the user turns it back on.

**Safe updates and data migrations.** Verify the new package first. Keep the old version until the new one starts and its data migration finishes in one transaction. Take a backup before any change of the plugin's data version. Roll back on failure.

**Uninstall without data loss.** "Remove" hides the plugin and keeps its data and its package. "Delete data" is a separate, confirmed action. Backups include the plugin packages, so a restore works offline.

---

## 8. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Bazaar reads the plugin store as «تغییر برنامه از روش‌های دیگر» and rejects the build | Medium | High | Ask Bazaar support in writing before the store ships. Until they answer, the Bazaar build ships built-in plugins only. |
| A Play reviewer flags plugin downloads | Low | High | The interpreter exception [S1]; review plugins against Play's policies; no `addJavascriptInterface`; a per-store catalog. |
| QuickJS in an isolated process is too slow on the Poco | Medium | Medium | The spike measures it first. Plan B is JavaScriptSandbox, in the same architecture. |
| HyperOS kills the plugin process mid-screen | Medium | Low | Restart on the next use; plugin state lives in the host database; nothing runs in the background. |
| Zipline's Kotlin coupling slows Kotlin upgrades | Medium | Low | Upgrade Kotlin when Zipline does (it has kept pace within weeks); if that ever fails, swap in quickjs-kt [S58] behind the same contract. |
| A level ③ WebView leaks data through WebSocket or WebRTC | Medium (if built) | High | Don't build level ③ until a plugin needs it; if built, every layer in §5 plus tests that try each channel. |
| No outside developers come | High | Low | Keep the platform small. The workout plugin and one sample plugin prove it; the store comes after v1.0. |
| Contract churn breaks plugins | Medium | Medium | Integer `hostApi` levels, additive changes only, one level of deprecation before removal. |
| The store host in Iran is unreachable for Play users abroad | Medium | Medium | Packages are signed, so mirror them anywhere. |
| One person maintains the host, SDK, store and backend | High | Medium | Small slices; the store only after the runtime proves itself. |
| **Play build, not plugins:** exact alarms are meant for apps whose core function needs them, "such as for an alarm clock app or a calendar app" [S56] ⚠️ | Medium | High (reminders) | Check `USE_EXACT_ALARM` and the battery-optimization request against Play's permission policies before any Play release. |

---

## 9. Verdict and the one recommended architecture

**Achievable: yes. Native plugin code from your own store, under both stores: no.** Native code survives only for built-in plugins (the workout) and for plugins published as separate apps on each store, which you don't want.

**The architecture: sandboxed scripts, host-drawn UI, declarative contributions.**
1. A plugin is a signed package: `manifest.json`, one JavaScript bundle, `strings/<lang>.json` and assets. Authors write Kotlin with your SDK (compiled to JavaScript), or TypeScript.
2. Plugin logic runs in QuickJS inside an `isolatedProcess` service of your app: no permissions, no network, no files. One process per running plugin on Android 10+, one shared process on Android 8–9.
3. The host API is a small set of calls over Binder: the attached goal, tasks, completions, plugin storage, metrics, photos, haptics, date helpers. Every call is checked against granted permissions and scoped to the plugin's own data.
4. UI is a tree. Level ①: your components. Level ②: free layout with your tokens. The host draws both with Compose, so language, direction, digits, theme, back and touch targets are right by construction.
5. Contributions live in the manifest, so most entries show without starting the plugin. Today cards come from snapshots the plugin publishes. One tap on Today is a host task tick, with no plugin involved.
6. Plugins never run in the background.
7. Built-in plugins (the workout) are Kotlin modules that implement the same contract inside the app.
8. Only `:store` uses the network. Its WebView talks to the app through `addWebMessageListener`, limited to your exact store origin.

**Why this one.** It is the only option that Play allows in writing and that Bazaar likely allows. It keeps every rule on your list enforceable by code. It keeps plugin data in your backup. It works on every phone from Android 8 without waiting for a WebView update. And authors can still write Kotlin.

---

## 10. What I couldn't verify

- **The 2026 text of Bazaar's rules.** The site returns 403 outside Iran; I used the April 2024 archive. Open https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/security from Iran and compare.
- **Whether Bazaar accepts in-app script plugins.** No written rule. Ask them.
- **Start time, memory and APK size** of QuickJS in an isolated process, and of JavaScriptSandbox, on the Poco.
- **How HyperOS treats an app's isolated processes.**
- **Whether WebView's network block covers WebSockets and WebRTC.** Traced only for URL loads.
- **Store presence:** Mihon not on Play; Obsidian and Joplin on Play; current production use of Zipline at Cash App.
- **Google's SDK Runtime** (Privacy Sandbox on Android): Google says "Some Privacy Sandbox technologies are being phased out" [S57] ✅, without saying which. Not used here.
- **That Android's `SHA256withECDSA` reads the DER form** (standard Java behavior). Confirm with a .NET-signed test vector in the spike.
- **The API level of `useAppZygote`** (29, from memory).
- **Play's current wording on exact alarms.** Only the Android docs' wording was found [S56].

---

## Sources

- [S1] Google Play Console Help, "Device and Network Abuse" (the old link `answer/9888379` redirects here): https://support.google.com/googleplay/android-developer/answer/16559646
- [S2] Google Play Console Help, "Use of the REQUEST_INSTALL_PACKAGES permission": https://support.google.com/googleplay/android-developer/answer/12085295
- [S3] Android Developers Blog, "Android developer verification: Building a safer ecosystem together", 2026-06-18: https://android-developers.googleblog.com/2026/06/android-developer-verification.html
- [S4] Android developer verification, overview and timeline: https://developer.android.com/developer-verification
- [S5] Android developer verification, FAQ (sanctioned countries, advanced flow, ADB): https://developer.android.com/developer-verification/guides/faq
- [S6] VS Code, "Extension Host": https://code.visualstudio.com/api/advanced-topics/extension-host
- [S7] VS Code, "Extension Manifest": https://code.visualstudio.com/api/references/extension-manifest
- [S8] VS Code, "Activation Events": https://code.visualstudio.com/api/references/activation-events
- [S9] VS Code, "Publishing Extensions": https://code.visualstudio.com/api/working-with-extensions/publishing-extension
- [S10] VS Code, "Extension runtime security": https://code.visualstudio.com/docs/configure/extensions/extension-runtime-security
- [S11] microsoft/vscode-l10n: https://github.com/microsoft/vscode-l10n
- [S12] VS Code, "Webview API": https://code.visualstudio.com/api/extension-guides/webview
- [S13] VS Code, "Web Extensions": https://code.visualstudio.com/api/extension-guides/web-extensions
- [S14] cashapp/zipline: README, CHANGELOG, `zipline/src/hostMain/.../QuickJs.kt`, `InterruptHandler.kt`, `zipline-loader/build.gradle.kts`: https://github.com/cashapp/zipline
- [S15] cashapp/redwood README: https://github.com/cashapp/redwood
- [S16] Android, `<service>` manifest element: https://developer.android.com/guide/topics/manifest/service-element
- [S17] Android, "Package visibility filtering": https://developer.android.com/training/package-visibility
- [S18] Android reference, `InMemoryDexClassLoader`: https://developer.android.com/reference/dalvik/system/InMemoryDexClassLoader
- [S19] Cafe Bazaar, «امنیت» (publishing rules), archived 2024-04-18: https://web.archive.org/web/20240418110809/https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/security · 2021 copy: https://web.archive.org/web/20210920225310/https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/security · live (Iran only): https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/security
- [S20] Cafe Bazaar, «تاریخچه بروزرسانی قوانین», archived 2024-05-29: https://web.archive.org/web/20240529181049/https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/policy-updates
- [S21] Cafe Bazaar, «پرداخت», archived 2024-07-19: https://web.archive.org/web/20240719124936/https://developers.cafebazaar.ir/fa/app-publish-guidelines/rules/payment
- [S22] Android 14, "Behavior changes: apps targeting Android 14": https://developer.android.com/about/versions/14/behavior-changes-14
- [S23] AOSP, `core/res/res/values/attrs_manifest.xml` (`useAppZygote`, `allowSharedIsolatedProcess`, `externalService`, `zygotePreloadName`): https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/res/values/attrs_manifest.xml
- [S24] Figma, "How to build a plugin system on the web and also sleep well at night", 2019-08-22: https://www.figma.com/blog/how-we-built-the-figma-plugin-system/
- [S25] Figma, "An update on plugin security", 2019-10-02: https://www.figma.com/blog/an-update-on-plugin-security/
- [S26] Android, "Execute JavaScript and WebAssembly" (JavaScriptEngine): https://developer.android.com/develop/ui/views/layout/webapps/jsengine
- [S27] Tasker, "Plugins": https://tasker.joaoapps.com/plugins.html
- [S28] Muzei, example source manifest: https://github.com/muzei/muzei/blob/main/example-unsplash/src/main/AndroidManifest.xml
- [S29] Android, "Create an input method": https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method
- [S30] Android, "App widgets overview": https://developer.android.com/develop/ui/views/appwidgets
- [S31] Termux wiki, "Termux and Android 10": https://github.com/termux/termux-packages/wiki/Termux-and-Android-10
- [S32] Mihon, `ExtensionLoader.kt`: https://github.com/mihonapp/mihon/blob/main/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt
- [S33] Obsidian Help, "Plugin security": https://obsidian.md/help/plugin-security
- [S34] Joplin Forum, "Mobile: Plugin support": https://discourse.joplinapp.org/t/mobile-plugin-support/35262
- [S35] Firefox Extension Workshop, "Signing and distribution overview": https://extensionworkshop.com/documentation/publish/signing-and-distribution-overview/
- [S36] Android Authority, "Kiwi Browser shutting down": https://www.androidauthority.com/kiwi-browser-shutting-down-3519510/
- [S37] Android for Cars, "Use the Android for Cars App Library": https://developer.android.com/training/cars/apps
- [S38] Android reference, `Context` (`bindIsolatedService`, `BIND_EXTERNAL_SERVICE`, `BIND_SHARED_ISOLATED_PROCESS`): https://developer.android.com/reference/android/content/Context
- [S39] GitHub repositories and release data, read 2026-10-22: https://github.com/quickjs-ng/quickjs · https://github.com/facebook/hermes · https://github.com/eclipsesource/J2V8 · https://github.com/caoccao/Javet · https://github.com/CharlieTap/chasm
- [S40] AndroidX source, `JavaScriptSandbox.java`: https://github.com/androidx/androidx/blob/androidx-main/javascriptengine/javascriptengine/src/main/java/androidx/javascriptengine/JavaScriptSandbox.java
- [S41] Chromium, WebView manifest expectations (`JsSandboxService0`): https://chromium.googlesource.com/chromium/src/+/main/android_webview/expectations/system_webview_64_bundle.AndroidManifest.expected
- [S42] AndroidX, JavaScriptEngine release notes: https://developer.android.com/jetpack/androidx/releases/javascriptengine
- [S43] Kotlin, "Kotlin/Wasm": https://kotlinlang.org/docs/wasm-overview.html
- [S44] QuickJS source (`quickjs.c`: no `"Intl"`; `JS_SetMemoryLimit`, `JS_SetInterruptHandler`): https://github.com/bellard/quickjs · https://github.com/quickjs-ng/quickjs
- [S45] Android reference, `SurfaceControlViewHost` (added in API 30), and its AOSP source: https://developer.android.com/reference/android/view/SurfaceControlViewHost · https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/SurfaceControlViewHost.java
- [S46] Android, "Darken web content in WebView": https://developer.android.com/develop/ui/views/layout/webapps/dark-theme
- [S47] AndroidX source, `WebViewCompat.addWebMessageListener`: https://github.com/androidx/androidx/blob/androidx-main/webkit/webkit/src/main/java/androidx/webkit/WebViewCompat.java
- [S48] AOSP, `WebSettings.java` (`setBlockNetworkLoads`, `setAllowFileAccess`): https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/webkit/WebSettings.java
- [S49] Chromium, `android_webview/browser/network_service/net_helpers.cc` (`UpdateLoadFlags`): https://chromium.googlesource.com/chromium/src/+/main/android_webview/browser/network_service/net_helpers.cc
- [S50] Android 8.0, "Behavior changes": https://developer.android.com/about/versions/oreo/android-8.0-changes
- [S51] Android reference, `java.security.Signature` (algorithms by API level): https://developer.android.com/reference/java/security/Signature
- [S52] Android 16, "Behavior changes: apps targeting Android 16": https://developer.android.com/about/versions/16/behavior-changes-16
- [S53] Android, "Per-app language preferences": https://developer.android.com/guide/topics/resources/app-languages
- [S54] Don't Kill My App, "Xiaomi": https://dontkillmyapp.com/xiaomi
- [S55] .NET API, `DSASignatureFormat`: https://learn.microsoft.com/en-us/dotnet/api/system.security.cryptography.dsasignatureformat
- [S56] Android, "Schedule alarms" (exact alarms; seen through search, not re-read): https://developer.android.com/training/scheduling/alarms
- [S57] Privacy Sandbox, "Android progress updates": https://privacysandbox.google.com/overview/android-progress-updates
- [S58] dokar3/quickjs-kt, README and `QuickJs.kt`: https://github.com/dokar3/quickjs-kt
- [S59] .NET API, `ECDsa.SignData`: https://learn.microsoft.com/en-us/dotnet/api/system.security.cryptography.ecdsa.signdata
- [S60] Android reference, `android.os.Process` (`isIsolated`): https://developer.android.com/reference/android/os/Process
