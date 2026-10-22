# Plugin platform: the plan

_Written 2026-10-22 from `docs/research/plugin-platform-research.md` (cited below as «research §n»). It refines the brief `docs/tasks/plugin-platform.md` (P0–P6). **Round 1 of §14 was answered on 2026-10-22**, with the recommended option each time: D177–D181 in `DECISIONS.md`. Round 2 is still open. In P0 the architecture goes into `ARCHITECTURE.md` and the contract into `docs/plugins/CONTRACT.md`._

## 0. In short

- **Plugins are signed script packages.** Authors write Kotlin with our SDK; it compiles to JavaScript (D179).
- **Plugin code runs in QuickJS (through Zipline) inside an Android isolated process.** That process has no permissions: no network, no files, no app data. D177 refines D166 this way, because Zipline alone "should not be used to execute untrusted code" (research §0).
- **The host draws all plugin UI with our own Compose components.** Level ① is our components. Level ② is a free layout (rows, columns, text, images, a canvas) that can only use our tokens (D178). A WebView comes later, as level ③, only if a real plugin needs it.
- **Plugins never run in the background.** Their daily actions are normal tasks; ticking one never starts the plugin.
- **The workout is a built-in native plugin.** Its code moves into its own module and implements the same contract. Its data stays in its current tables (D180).
- **Removing a plugin keeps its data** (D181).
- **The store comes after v1.0**, as the brief already says. Only `:store` touches the network.

---

## 1. Architecture

### 1.1 Modules

| Module | Kind | Holds | May use |
|---|---|---|---|
| `:domain` | Kotlin/JVM | Rules, Jalali, scoring (unchanged) | No Android |
| `:plugin-api` | Kotlin Multiplatform (JVM + JS) | The manifest, the UI tree, events, the host services as Kotlin interfaces, permission ids, validation rules | Zipline's `ZiplineService` type, kotlinx.serialization |
| `:design` | Android library | Theme tokens and shared components, moved out of `:app` (`ui/theme`, `ui/components`) | Compose |
| `:plugin-host` | Android library | Registry, verifier and installer, permission broker, plugin storage, quotas, backup adapters, the UI-tree renderer, the runtime client | `:plugin-api`, `:design`, Room. **No network** |
| `:plugin-runtime` | Android library | The isolated-process service, Zipline, the Binder bridge | `:plugin-api`, Zipline (not `zipline-loader`). **No Room, no network** |
| `:plugin-workout` | Android library | The workout as a built-in plugin (moved code) | `:plugin-api`, `:design`, `:domain`, its own DAOs |
| `:store` | Android library | The store WebView, the catalog client, package downloads | **The network (the only module)** |
| `:app` | Android app | Wiring (`AppContainer`), navigation, Today, goals, settings, the Room database | All of the above |

The Room database stays in `:app`. It lists entity classes from `:plugin-host` and `:plugin-workout`, so there is still one database and one backup.

### 1.2 Processes

```
┌──────────────────────── main process (the app) ───────────────────────────┐
│  :app          Today · Goals · Settings · the Room database               │
│  :plugin-host  registry · permissions · storage · renderer (Compose)      │
│  :plugin-workout   built-in plugin, runs here (trusted, in the APK)        │
│  :store        the only network code: store WebView + downloads           │
│        │  ▲                                                               │
│        │  │  UI trees and events, host-API calls (Binder, scoped per plugin)│
└────────┼──┼───────────────────────────────────────────────────────────────┘
         ▼  │   bindIsolatedService(instanceName = plugin id)   (Android 10+)
┌───────────────── isolated process, one per running plugin ─────────────────┐
│  :plugin-runtime   Zipline → QuickJS ← the plugin's verified main.js       │
│  no permissions · no network · no app files · killed when idle             │
└────────────────────────────────────────────────────────────────────────────┘
```

- **Android 10 and newer:** one isolated process per running script plugin (`bindIsolatedService`, research §6.2).
- **Android 8 and 9:** one shared isolated process, with a separate Zipline instance (its own QuickJS runtime) per plugin.
- **Plugin files reach the isolated process as a read-only file descriptor** from the main process. The isolated process can't open the app's files itself.
- `App.onCreate` runs in every process. In the isolated one it must skip the database, alarms and WorkManager (`Process.isIsolated()`, API 28+; the process name below that).

### 1.3 Build checks

- **`checkNetworkFence`** (replaces D60's manifest check, as D167 says). It fails when a module other than `:store`:
  - depends on a network library (OkHttp, Ktor, Retrofit, Volley, Cronet, `zipline-loader`), or
  - has compiled classes that reference `java.net.Socket`, `java.net.URL.openConnection`, `java.net.HttpURLConnection`, `javax.net.ssl`, `android.net.http` or `android.webkit.WebView`.
  It scans our modules' bytecode, not third-party libraries (they would give false alarms); libraries are covered by the dependency list. `INTERNET` is declared only in `:store`'s manifest.
- **`checkBannedDependencies`** (exists): add `app.cash.zipline:zipline-loader`.
- `:plugin-api` can't import Android: it's Kotlin Multiplatform, so the compiler enforces it.

---

## 2. How a plugin runs

### 2.1 States

`Installed` → `Enabled` (attached to a goal) → `Running` (its process is up) → `Idle` (process unbound 30 s after its last visible surface) → back to `Running` on the next use.
Side states: `Paused` (3 crashes in a day; the user turns it back on), `Removed` (hidden; package and data kept), `Data deleted` (a separate, confirmed step).

### 2.2 When it starts (activation, like VS Code)

| Starts the plugin | Doesn't start it |
|---|---|
| Opening one of its screens, its goal section, its goal-flow step, its chapter-review part, its settings | Ticking its tasks on Today, the widget, notifications |
| Today opens and the plugin has no card snapshot for today | Boot, alarms, backups, restores |
| Updating it (to run its data migration) | Anything while the app is in the background |

### 2.3 Today without the plugin

- A plugin's daily actions are **normal host tasks** linked to the goal. They show as tiles, tick in one tap, count in the ring and get reminders from the host. No plugin code runs.
- A plugin's **Today card** is a snapshot. When it runs, the plugin publishes cards for up to 7 days ahead (`UiService.publishTodayCards`). Today shows the one for today.
- A snapshot can contain **live host nodes** (`TaskTile`, `WeekBand`), so it stays correct after a tick without asking the plugin.

### 2.4 Limits

| Limit | Default | Where |
|---|---|---|
| Memory per plugin | 32 MB QuickJS heap | `zipline.quickJs.memoryLimit` |
| Time per call | 200 ms target, cut at 1 s | Zipline `interruptHandler` + a Binder timeout |
| Stored documents | 2 MB per plugin | `plugin_doc` quota |
| Files (photos) | 200 MB per plugin | `plugin_file` quota |
| Tasks | 10 per plugin | host check |
| Today cards | 7 days ahead | host check |
| Crashes | 3 in a day → `Paused` | runtime client |

All defaults are starting points; the spike (S0) sets the real ones.

---

## 3. Extension points (contribution points)

Every point is **declared in the manifest**, so the host knows about it without running the plugin.

| Point | The plugin gives | The host does | Runs plugin code? | Workout example |
|---|---|---|---|---|
| `goalTemplate` | Pre-fill: goal type, lead and lag metrics, a week range, flex days | Shows a chip in step ④ of the goal flow | No | «تناسب اندام» |
| `goalFlowSteps` | Extra steps and where they go | Inserts them into the 8-step flow | Yes, to draw the step | «برنامه‌ی تمرین» after step ④ |
| `dailyActions` | Default actions (title key, minimum, part of the day) | Creates normal tasks linked to the goal | No | «تمرین» |
| `todayCard` | Card snapshots | Shows today's card above the part switcher | Only to refresh snapshots | «تمرین امروز» |
| `goalSections` | Sections for the goal's page | Places them on the goal detail | Yes (lazily, cached) | Records, ladders, totals |
| `screens` | Full screens | Host routes with the host's top bar and back | Yes | The session, the plan editor |
| `chapterReview` | A part of the monthly review | Adds it to the review flow | Yes | The check-in with the ghost camera |
| `metrics` | Lead and lag metrics | Draws them with host charts | No | Sessions a week, weight, tape |
| `settings` | A settings screen | An entry under Settings → «افزونه‌ها» | Yes | Workout settings |

---

## 4. The UI levels

The host owns: navigation and back, insets and edge-to-edge, layout direction, language, digits, dates, fonts, colors, light and dark, motion specs, haptics, minimum touch sizes, and the calm error message.

### 4.1 Level ①: host components (the default)

A plugin returns a tree of our components; the host draws them with the code in `:design`.

`Card`, `TaskTile` (live), `WeekBand` (live), `Band`, `ChapterPath`, `Stepper`, `Counter`, `Effort` (the 1–10 seek bar), `LineChart`, `Sparkline`, `BarChart`, `ListRow`, `SectionHeader`, `Chip`, `ChipGroup`, `Button` (primary, secondary, ghost), `Sheet`, `PhotoSlot` (with the host camera and an optional ghost overlay), `Timer` (host-driven), `Banner` (calm info), `EmptyState`, `EndLayout`.

The rules hold by construction: there is no color, font or raw-number input anywhere.

### 4.2 Level ②: free layout (the plugin's own UI)

For screens our components don't cover. Still drawn by the host.

- **Layout:** `Column`, `Row`, `Box`, `Scroll`, `LazyList`, `Pager`, `Spacer`. Spacing and padding use space tokens; sizes are dp; alignment is start, center, end. There is no left or right.
- **Content:** `Text` (a `Txt`, a type token, a color token, an alpha), `Icon` (host icons or the plugin's vector assets), `Image` (plugin assets or its photos).
- **Drawing:** `Canvas` with operations (line, rect, rounded rect, circle, arc, path) in token colors with alpha and stroke widths. It mirrors in RTL unless `mirrorInRtl = false` (for example, a numeric chart that reads left to right).
- **Input:** `Clickable`, `TextField`, `Toggle`, gestures declared on nodes (tap, long-press, drag with a value range). The host runs the gesture locally and sends one event when it ends, so dragging never waits on a Binder call.
- **Motion:** `Animated` with host motion specs only.

### 4.3 Level ③: a sandboxed WebView (later, only on demand)

Not in this plan's build order. If a real plugin needs it, it gets every layer from research §5: `setBlockNetworkLoads(true)`; `shouldInterceptRequest` that serves only the plugin's files through `WebViewAssetLoader`; a CSP with `connect-src 'none'`; `RTCPeerConnection` removed before the page runs; no file or content access; no navigation; the bridge through `addWebMessageListener` limited to the plugin's asset origin; our design kit as CSS variables, Estedad, `lang` and `dir` on `<html>`. Plus a harness test that tries each network channel and must fail. And it needs a decision of its own.

### 4.4 Back

- The host handles system back for every plugin screen (needed anyway: on Android 16, `onBackPressed` isn't called for apps targeting 36, research §6.3).
- A screen can set `backBehavior = CONFIRM_DISCARD`. Then the host shows its own sheet once («تغییراتت ذخیره نشده. بری بیرون؟»). A plugin can never block back.
- Process death: each render may return a small `savedState`. The host keeps it and hands it back on the next render.

---

## 5. Localization: two languages, both directions

- **The host is the only source** of the current language, layout direction, digit style and which calendar comes first (D174–D176). It passes them in `HostEnv`. A plugin never reads the phone's locale. (QuickJS has no `Intl` anyway, research §4.2 d.)
- **Strings:** `strings/<lang>.json` per language. Plain keys; placeholders `{0}`, `{1}`; plural forms by CLDR category (`one`, `other`…), chosen by the host.
- **Text nodes never hold plugin literals.** A `Txt` is a string key, a plural, a host-formatted number, date or duration, or the user's own words.
- **Numbers and dates are formatted by the host:** Persian digits in Persian; Jalali first in Persian; Gregorian first in English, with the swap from D176.
- **A language the plugin doesn't have:** the text falls back to the plugin's `defaultLanguage`. The layout direction stays the app's. Each text is isolated so its own direction reads right. The install sheet says so first («این افزونه فارسی نداره؛ متن‌هاش انگلیسیه.»).
- **The store** shows each plugin's languages, and needs a name and summary in each one.
- **Tests** cover four cases: fa-RTL, en-LTR, English text in the RTL app, Persian text in the LTR app.

---

## 6. Storage and backup

### 6.1 Tables (additive)

```
plugin_install  pluginId PK, versionCode, version, source (BUILT_IN|STORE|DEV), enabled, goalId?,
                grants (bits), packageSha256, dataVersion, installedAt, updatedAt,
                removedAt?, pausedAt?, crashCount, crashDay?
plugin_doc      pluginId, collection, key, json, updatedAt              PK (pluginId, collection, key)
plugin_file     pluginId, path, kind (PHOTO|ASSET), sizeBytes, createdAt PK (pluginId, path)
plugin_card     pluginId, date, treeJson, publishedAt                     PK (pluginId, date)   -- cache
plugin_state    pluginId, surfaceKey, json                                 PK (pluginId, surfaceKey) -- cache
task            + pluginId?, pluginActionId?, titleKey?
```

- `task.titleKey` keeps a plugin task's title as a string key, so it follows a language switch. When the user renames the task, `title` takes over and `titleKey` is cleared.
- On disk: `files/plugins/<id>/<versionCode>/…` (the package, read-only after install) and `files/plugin-data/<id>/…` (photos).

### 6.2 Backup and restore

- `data.json` gets `plugin_install`, `plugin_doc`, `plugin_file` and the new `task` columns. These are additive, so `formatVersion` stays 1 (`BACKUP_FORMAT.md`). The two caches are left out, like `backup_record`.
- The zip gets `plugins/<id>-<versionCode>.plugin` (the signed packages) and `plugin-data/<id>/…`. `manifest.json` lists them with their sizes.
- **Restore** re-verifies every package. One that fails (for example, a developer-signed package in a release build) stays disabled, and its data is kept.
- So a restore on a new phone brings back every plugin **offline**.

---

## 7. Install, update, remove, sign

### 7.1 The package

A zip named `<id>-<versionCode>.plugin`:

```
manifest.json
main.js              the plugin, ES2020, one file (source, not bytecode)
strings/fa.json      one file per language in manifest.languages
strings/en.json
assets/…             icons (vector JSON), images (PNG/WebP)
review.json          added by the store: channels it was approved for, review date
files.json           every file above with its SHA-256 and size
signature.json       keyId + an ECDSA P-256 / SHA-256 signature over files.json (DER)
```

The package ships JavaScript source. The device compiles it with its own QuickJS, so a package never breaks when the app's engine version changes.

### 7.2 Verify (in `:plugin-host`, never in `:store`)

1. The signature over `files.json` checks out with one of the two pinned store keys (or the developer key, in debug builds only).
2. Every file matches its hash and size; any file not on the list fails the package.
3. `manifest.json` parses; `id` and `versionCode` match what was asked for; `minHostApi` ≤ our level.
4. Each language in `languages` has its strings file with every key the plugin uses (rule L2).
5. Size limits hold; the permissions are known.
6. The version isn't on the revocation list.

### 7.3 Install, update, remove

- **Install:** the native consent sheet shows the name, author, version, languages, permissions (each in plain words) and size, in the app's language. «نصب» writes the files into a temporary folder, marks them read-only, then moves them into place and inserts `plugin_install` in one step.
- **Update:** verify, show what changed (permissions first), ask. If `dataVersion` goes up: back up first, start the new version, run `migrate(from, to)` with its writes in one host transaction, then switch. On failure: roll back and keep the old version.
- **Remove:** hides the plugin and keeps its package and data. «پاک کردن داده‌ها» is a separate, confirmed step. Installing again restores everything.
- **Revocation:** a signed list, fetched when the store screen opens. A revoked version pauses with a calm message and offers the update. Data is kept.
- **Developer sideload:** debug builds only. A file picker in Developer mode, or `adb push` into a dev folder; checked against the developer key.

### 7.4 Signing

- **ECDSA P-256 with SHA-256.** Android verifies it on every API level we support (`SHA256withECDSA`, API 11+; `Ed25519` only from 33; research §7).
- **The store** signs with .NET: `ECDsa.SignData(bytes, HashAlgorithmName.SHA256, DSASignatureFormat.Rfc3279DerSequence)`, the DER form Android expects. The spike confirms this with a test vector.
- **Keys:** two public keys pinned in the app (current and next, with a `keyId`), so the key can rotate. The private key lives on the user's laptop in a signing tool, never on the web server.

---

## 8. The draft contract

### 8.1 The manifest

```json
{
  "schema": 1,
  "id": "ir.example.readinglog",
  "version": "1.0.0",
  "versionCode": 1,
  "minHostApi": 1,
  "dataVersion": 1,
  "languages": ["fa", "en"],
  "defaultLanguage": "fa",
  "name": "@name",
  "summary": "@summary",
  "author": { "name": "Example Dev", "contact": "dev@example.ir" },
  "icon": "assets/icon.json",
  "goalTypes": ["HABIT", "SKILL"],
  "permissions": ["METRICS"],
  "uiLevels": [1, 2],
  "contributes": {
    "goalTemplate": {
      "title": "@template_title",
      "goalType": "HABIT",
      "lead": { "name": "@lead_pages", "kind": "AMOUNT", "unit": "@unit_pages" },
      "weekRange": { "low": 3, "high": 5 }
    },
    "dailyActions": [
      { "id": "read", "title": "@action_read", "minimum": "@action_read_min", "part": "EVENING" }
    ],
    "todayCard": true,
    "goalSections": [{ "id": "books", "title": "@section_books" }],
    "screens": [{ "id": "book", "title": "@screen_book" }],
    "metrics": [
      { "key": "pages", "name": "@metric_pages", "unit": "@unit_pages", "role": "LEAD", "higherIsBetter": true }
    ],
    "settings": { "screen": "settings" }
  },
  "limits": { "docsKb": 512, "tasks": 3 }
}
```

`"@key"` points into the strings files, like VS Code's `%key%`. `strings/fa.json` for the same plugin:

```json
{
  "name": "دفتر کتاب‌خوانی",
  "summary": "هر شب چند صفحه، و کتاب‌هایی که تموم کردی.",
  "action_read": "کتاب بخونم",
  "action_read_min": "حداقل {0}",
  "unit_pages": { "one": "صفحه", "other": "صفحه" },
  "pages_this_week": { "one": "این هفته {0} صفحه", "other": "این هفته {0} صفحه" }
}
```

### 8.2 The host API as Kotlin interfaces

In `:plugin-api` (`commonMain`). Each interface extends `ZiplineService`, so Zipline bridges it inside the isolated process. The built-in workout plugin calls the same interfaces directly.

```kotlin
package app.beaesthetic.plugin.api

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Raised only when the contract changes in a way old plugins can't use. Additions stay inside a level. */
const val HOST_API_LEVEL = 1

/** What a plugin implements. Dates are ISO strings ("2026-10-22"), Gregorian, after the 04:00 rollover. */
interface PluginService : ZiplineService {
    /** Called once per process start, before anything else. */
    suspend fun activate(env: HostEnv)

    suspend fun render(surface: Surface): Rendered

    /** null = nothing changed. */
    suspend fun onEvent(event: UiEvent): Rendered?

    /** Moves this plugin's documents from one data version to the next, one step at a time. */
    suspend fun migrate(fromVersion: Int, toVersion: Int)
}

@Serializable
data class HostEnv(
    val hostApi: Int,
    val language: String,       // "fa" or "en": the app's language, never the phone's
    val rtl: Boolean,
    val persianDigits: Boolean,
    val jalaliFirst: Boolean,
    val dark: Boolean,
    val accent: Accent,
    val fontScale: Float,
    val today: String,
    val grants: Set<Permission>,
)

@Serializable
data class Rendered(
    val tree: UiNode,
    val savedState: JsonObject? = null,
    val backBehavior: BackBehavior = BackBehavior.LEAVE,
)

/** Granted to every plugin. Only the goal this plugin is attached to. */
interface GoalService : ZiplineService {
    suspend fun goal(): GoalView
    suspend fun weeks(range: DateRange): List<GoalWeekView>
}

/** Granted to every plugin. Only the tasks this plugin created. */
interface TaskService : ZiplineService {
    suspend fun tasks(): List<TaskView>
    suspend fun create(spec: TaskSpec): String
    suspend fun update(id: String, spec: TaskSpec)
    suspend fun archive(id: String)
    suspend fun completions(range: DateRange): List<CompletionView>

    /** The host's edit window applies: today and yesterday only. */
    suspend fun complete(id: String, date: String, amount: Double? = null, note: String? = null)
}

/** Granted to every plugin. Only this plugin's documents, inside its quota. */
interface StorageService : ZiplineService {
    suspend fun get(collection: String, key: String): JsonElement?
    suspend fun list(collection: String, query: DocQuery = DocQuery()): DocPage

    /** All operations in one call commit together, or none do. */
    suspend fun write(ops: List<DocOp>)
}

/** Permission METRICS. Only the metrics in this plugin's manifest. */
interface MetricService : ZiplineService {
    suspend fun record(key: String, date: String, value: Double)
    suspend fun series(key: String, range: DateRange): List<MetricPoint>
}

/** Permission PHOTOS; capture() also needs CAMERA. Only this plugin's albums. */
interface PhotoService : ZiplineService {
    /** Opens the host's camera screen, optionally with a faint earlier photo over the preview. null = cancelled. */
    suspend fun capture(request: CaptureRequest): PhotoRef?
    suspend fun pick(): PhotoRef?
    suspend fun list(album: String): List<PhotoRef>
    suspend fun delete(ref: PhotoRef)
}

/** Granted to every plugin. */
interface UiService : ZiplineService {
    /** Only screens declared in the manifest. */
    suspend fun open(screenId: String, args: JsonObject = JsonObject(emptyMap()))
    suspend fun close()

    /** Up to 7 days ahead, keyed by date. Today shows them without starting the plugin. */
    suspend fun publishTodayCards(cards: Map<String, UiNode>)
    suspend fun invalidate(surface: SurfaceKey)
    fun haptic(kind: Haptic)
}

/** Granted to every plugin. The app's own date rules. */
interface TimeService : ZiplineService {
    suspend fun jalali(date: String): JalaliDate
    suspend fun weekStart(date: String): String   // Saturday
    suspend fun chapterOf(date: String): ChapterView?
}

@Serializable
enum class Permission { METRICS, PHOTOS, CAMERA, REMINDERS }
```

A service the plugin wasn't granted is never bound, so calling it fails. The main process checks again on every call, using the plugin identity of the Binder connection, never anything the plugin sends.

### 8.3 Surfaces, events and the UI tree

```kotlin
@Serializable
sealed interface Surface {
    @Serializable data class TodayCard(val date: String) : Surface
    @Serializable data class GoalSection(val sectionId: String) : Surface
    @Serializable data class Screen(val screenId: String, val args: JsonObject, val savedState: JsonObject?) : Surface
    @Serializable data class FlowStep(val stepId: String, val draft: GoalDraftView) : Surface
    @Serializable data class ReviewPart(val partId: String, val chapter: ChapterView) : Surface
    @Serializable data class Settings(val screenId: String) : Surface
}

@Serializable
sealed interface UiEvent {
    val surface: SurfaceKey

    @Serializable data class Click(override val surface: SurfaceKey, val nodeId: String) : UiEvent
    @Serializable data class Change(override val surface: SurfaceKey, val nodeId: String, val value: JsonElement) : UiEvent
    @Serializable data class Back(override val surface: SurfaceKey) : UiEvent
}

/** Text is never a plugin literal. */
@Serializable
sealed interface Txt {
    @Serializable data class Res(val key: String, val args: List<Txt> = emptyList()) : Txt
    @Serializable data class Plural(val key: String, val count: Long, val args: List<Txt> = emptyList()) : Txt
    @Serializable data class Num(val value: Double, val decimals: Int = 0, val unitKey: String? = null) : Txt
    @Serializable data class Day(val date: String, val style: DateStyle = DateStyle.SHORT) : Txt
    @Serializable data class Duration(val seconds: Long) : Txt

    /** The user's own words, shown as typed. */
    @Serializable data class User(val text: String) : Txt
}

/** No red. No color values. */
@Serializable
enum class ColorToken { ACCENT, ACCENT_SOFT, ON_ACCENT, AMBER, AMBER_SOFT, INDIGO, INDIGO_SOFT, GAP, TEXT, TEXT_MUTED, SURFACE, SURFACE_RAISED, BORDER }

@Serializable
sealed interface UiNode {
    val id: String?
}

// Level ①, for example:
@Serializable data class Card(override val id: String? = null, val title: Txt, val body: List<UiNode> = emptyList(), val onClick: Boolean = false) : UiNode
@Serializable data class TaskTile(override val id: String? = null, val taskId: String) : UiNode
@Serializable data class Stepper(override val id: String, val value: Double, val min: Double, val max: Double, val step: Double, val unitKey: String? = null) : UiNode
@Serializable data class LineChart(override val id: String? = null, val points: List<ChartPoint>, val rollingAverage: Boolean = false, val unitKey: String? = null) : UiNode
@Serializable data class Button(override val id: String, val label: Txt, val style: ButtonStyle = ButtonStyle.PRIMARY) : UiNode

// Level ②, for example:
@Serializable data class Column(override val id: String? = null, val children: List<UiNode>, val gap: Space = Space.M, val align: Align = Align.START) : UiNode
@Serializable data class Text(override val id: String? = null, val text: Txt, val style: TypeToken = TypeToken.BODY, val color: ColorToken = ColorToken.TEXT, val alpha: Float = 1f) : UiNode
@Serializable data class Canvas(override val id: String? = null, val widthDp: Int, val heightDp: Int, val ops: List<DrawOp>, val label: Txt?, val mirrorInRtl: Boolean = true) : UiNode
@Serializable data class Clickable(override val id: String, val child: UiNode, val label: Txt) : UiNode
```

The full node list goes into `docs/plugins/CONTRACT.md`, generated from the KDoc in `:plugin-api`.

### 8.4 Permissions

| Permission | Gives | Install sheet (en / fa) |
|---|---|---|
| _(always)_ | The attached goal, its own tasks, its own storage, UI, time helpers, haptics | — |
| `METRICS` | Write and read the metrics in its manifest | "Records numbers for your goal" / «برای هدفت عدد ثبت می‌کنه» |
| `PHOTOS` | Keep and show photos in its own albums | "Keeps photos for your goal" / «برای هدفت عکس نگه می‌داره» |
| `CAMERA` | Open the host's camera screen (needs `PHOTOS`) | "Uses the camera" / «از دوربین استفاده می‌کنه» |
| `REMINDERS` | Its tasks may have their own alarm | "Sets reminders" / «یادآور می‌ذاره» |

The Android camera permission is asked by the host, the first time the camera opens. The Persian lines are drafts; read them aloud before they ship.

### 8.5 Versioning

- **`HOST_API_LEVEL`** is an integer, starting at 1. Inside a level, changes are additive only: new optional fields, new nodes, new methods.
- **A plugin runs on any host whose level is ≥ its `minHostApi`.** A higher `minHostApi` gets a calm «این افزونه نسخه‌ی تازه‌تر اپ رو لازم داره».
- **`pluginCheck`** fails when a plugin uses something newer than its `minHostApi`.
- **Removing** anything takes two levels: deprecated in N, removed in N + 2.
- **`dataVersion`** belongs to the plugin; the host calls `migrate` step by step.
- **`schema`** is the manifest format's own version.

---

## 9. The minimum UI standard, as testable rules

"By construction" = the API can't express a violation. "Check" = `pluginCheck` (static, in the SDK and on the store server). "Harness" = `pluginTest` and the device matrix (§10). "Review" = you.

| # | Rule | Enforced by |
|---|---|---|
| **L1** | Every visible text is a string key, a host-formatted value, or the user's own words. | By construction (`Txt`); check flags `Txt.User` built from a literal; review |
| **L2** | Every key used exists in every claimed language, with the same placeholders and plural forms. | Check; the host refuses the package |
| **L3** | Persian strings have no Latin digits; English strings have no Persian or Arabic-Indic digits. | Check |
| **L4** | The plugin never formats numbers or dates and never reads a locale. | By construction; check flags `toLocale*` and `Intl`; review |
| **L5** | No left or right: layout uses start and end; drawings mirror in RTL unless marked. | By construction; harness screenshots |
| **L6** | Every surface renders without clipping in fa-RTL, en-LTR, and both fallback cases. | Harness |
| **T1** | Colors come only from tokens; no red token exists. | By construction |
| **T2** | Light and dark both render; text keeps ≥ 4.5:1 contrast (≥ 3:1 for large text) after alpha, for every accent. | Harness, computed from the tokens |
| **T3** | Fonts are the host's, through type tokens. | By construction |
| **B1** | System back leaves any plugin screen in one press, or two with the host's discard sheet. | By construction; harness presses back on every screen |
| **B2** | A plugin opens only the screens in its manifest. | By construction |
| **A1** | Every clickable area is at least 48 × 48 dp. | By construction (the host pads it); harness checks bounds |
| **A2** | Every icon-only control, image and canvas has a label, or is marked decorative. | Check; harness; debug renders fail |
| **A3** | No clipped text at font scale 1.3 and 2.0. | Harness |
| **A4** | Screen-reader order follows reading order in both directions. | Harness; review |
| **D1** | No network. | By construction (isolated process, no API); check flags `fetch`, `XMLHttpRequest`, `WebSocket`, `importScripts` |
| **D2** | Data only through the host API, inside quotas. | By construction |
| **D3** | Each `dataVersion` step has a migration that passes on sample data from the previous version. | Harness; review |
| **D4** | No dynamic code: no `eval`, `new Function`, dynamic `import()`. | Check; review |
| **P1** | No red for misses; amber means "due" or "important", never "missed". | By construction; review |
| **P2** | No streaks that reset, no counts of days in a row. | No such component; check flags words for the reviewer (en: streak, chain, in a row; fa: «زنجیره»، «پشت سر هم»، «روز متوالی»); review |
| **P3** | No guilt wording. | Check flags words (en: failed, lazy, missed, behind; fa: «شکست»، «تنبل»، «از دست دادی»، «عقب افتادی»); review reads every string, the Persian aloud |
| **P4** | No lists of what was missed. | No such component; review |
| **P5** | Errors show the host's calm message, never the plugin's. | By construction |
| **S1** | `render` answers within 200 ms (p95) on the reference phone; a call over 1 s is cut. | Runtime limit; harness timings |
| **S2** | The package stays inside its size budget (set after the spike). | Check |

---

## 10. The developer kit

### 10.1 What authors get

- **SDK:** `plugin-api` (its JS target) and `plugin-sdk` (the glue: one call starts the plugin and binds its `PluginService`), on a Maven repository you host as static files.
- **Gradle plugin** `app.beaesthetic.plugin`. It applies Kotlin/JS and Zipline's Gradle plugin and adds four tasks:
  - `pluginBundle`: one `main.js`;
  - `pluginCheck`: the static rules in §9;
  - `pluginTest`: the JVM harness;
  - `pluginPack`: the `.plugin` file, signed with the developer key.
- **Starter project:** the reading log (manifest, both string files, one plugin class, tests). Clone, rename, build.
- **Docs, in English and Persian:** `docs/plugins/CONTRACT.md` (the contract) and `docs/plugins/GUIDELINES.md` (design, copy, data, performance, accessibility, the product rules, the review checklist).

### 10.2 The test harness

1. **`pluginTest` on the JVM.** It runs the plugin in Zipline on the JVM against a `FakeHost` (an in-memory goal, tasks, storage and photos). It renders every declared surface for fa and en, light and dark, and saves each UI tree as a JSON snapshot. It replays event scripts and runs each migration on fixtures. Covers L2–L4, A2, D1, D3, D4, and S1 as a rough guide.
2. **The device matrix.** In debug builds, Settings gets a Developer mode. It installs a developer-signed package and shows each surface in a grid: {fa-RTL, en-LTR, English text in RTL, Persian text in LTR} × {light, dark} × {font 1.0, 1.3, 2.0}. It runs the automatic checks (L6, T2, A1, A3, A4), exports screenshots, and writes a report you can attach to a submission.
3. **Later:** the same matrix as JVM screenshot tests in CI, through Robolectric with the `:design` renderer published as a library ❓ (the tool choice comes at S8).

### 10.3 The review checklist (for you, per submission)

1. **Identity:** a known author; the id matches; `versionCode` went up.
2. **The automatic report is green:** manifest, strings, sizes, banned code, word flags, harness timings and snapshots.
3. **Read the source diff** against the last approved version.
4. **Permissions:** each one used, nothing extra, the install text accurate.
5. **Product rules:** no red for misses; no streaks; no guilt; no lists of misses; moments informational, not generic. Read every string in both languages; read the Persian aloud.
6. **Design:** level ① where it fits; level ② screens follow `GUIDELINES.md`; look at the matrix screenshots.
7. **Data:** migrations present and tested; no growth without bounds.
8. **Store policies:** for the Play channel, Play's policies (deceptive behavior, health claims, user data); for the Bazaar channel, Bazaar's content rules.
9. **Listing:** name and summary in every claimed language; honest screenshots.
10. **Decision:** approve per channel, or send back with notes.

---

## 11. The store flow

### 11.1 App side (`:store`)

1. Settings → «افزونه‌ها» → «فروشگاه». The only online screen.
2. **The WebView:** HTTPS only; loads only the store origin (and its package mirror); every other navigation is blocked or opened outside the app; no file or content access; no `addJavascriptInterface`; cookies off; the cache cleared on exit (Bazaar's WebView advice, research §4.1).
3. **The page** lists plugins for the app's channel, host API level and language. The request carries only those three values: no device id, no user data.
4. **«نصب» on the page** posts `{type: "install", id, versionCode}`. The native side receives it through `addWebMessageListener`, limited to the exact store origin.
5. **Native:** fetch the catalog entry, download the package, hand the bytes to `:plugin-host`, which verifies them (§7.2) and shows the native consent sheet (§7.3).
6. **After install:** offer to attach the plugin to a goal: its template opens the goal flow, or pick an existing goal.
7. **Updates:** when the store screen opens, send the installed ids and `versionCode`s, show what's new, ask before each update.
8. **Revocations:** fetched with the catalog.

### 11.2 The two builds (product flavors)

| | `bazaar` (first) | `play` (later) |
|---|---|---|
| App updates | Through Bazaar | Through Play |
| Plugin catalog | `channel=bazaar` | `channel=play`: only versions reviewed against Play's policies |
| Store pages | Never name another store (Bazaar rule, research §4.1) | The same pages, no store names |
| Store screen | On once Bazaar confirms in writing (§14, round 2). Built-in plugins work either way | On at launch |
| Payments | None (D169). Later: Bazaar's billing | None. Later: Play Billing |
| Also to check | — | `USE_EXACT_ALARM`, the battery-optimization request, the Data safety form, a privacy policy (research §8) |

The plugin runtime, the contract and the packages are the same in both.

### 11.3 Backend side (.NET, its own repository, D170)

**Parts**
- **`Store.Api`** (ASP.NET Core): the public, read-only API and the package files.
- **`Store.Web`** (server-rendered pages): the store pages for the WebView (fa and en, RTL and LTR, light and dark through `prefers-color-scheme`, Estedad served from the store's own domain, no third-party scripts), the developer portal and your review console.
- **`Store.Checks`**: a worker that runs `pluginCheck` and `pluginTest` in a container with a JDK, and stores the report.
- **`Store.Signer`**: a command-line tool on your laptop. It holds the private key, signs approved versions and uploads `signature.json`. The server never holds the key.
- **A database and file storage**; optionally a package mirror abroad (packages are signed, so any mirror is safe).

**Public API**
```
GET  /api/v1/catalog?channel=bazaar&hostApi=1&lang=fa     list (name, summary, languages, version, size, permissions, icon)
GET  /api/v1/plugins/{id}?channel=…&lang=…                details and screenshots
GET  /packages/{id}-{versionCode}.plugin                    static, cacheable
POST /api/v1/updates   { hostApi, channel, installed: [{ id, versionCode }] }   → available updates
GET  /api/v1/revocations                                    a signed list
```

**Data model:** `Developer` (invite-only at first), `Plugin`, `PluginVersion` (status: submitted → checking → in review → approved per channel → signed → published, or sent back or revoked), `CheckReport`, `Review` (notes), `Signature` (`keyId`), `DownloadCount` (per version per day), `Revocation`.

**The flow:** upload (package + source zip) → automatic checks → your review with the checklist → approve per channel → sign on your laptop → publish → in the catalog.

**Privacy:** no user accounts, no cookies, no analytics; server logs kept 7 days at most; download counts only.

---

## 12. The workout as the first plugin, without losing data

**The shape.** `:plugin-workout` is a built-in native plugin. It implements `PluginService` in the main process, keeps its own Compose screens (a right only built-in plugins have), and uses the same host services as everyone else for goals, tasks and metrics.

**Code moves (no rewrite):**
- `ui/workout/*` (plan editor, session) and the workout parts of `ui/progress/*` (records, ladders, totals, strength tests, check-in, ghost camera, reveal) go to `:plugin-workout`.
- `WorkoutRepository` and the photo code go with them.
- The workout rules stay in `:domain`.
- About 4,600 lines move (research §1).

**Data stays where it is.** `exercise`, `workout`, `workout_exercise`, `session`, `set_log`, `round_number`, `check_in`, `photo` and the workout rows of `metric` and `metric_entry` don't move. They are already in every backup. The goal migration (SPEC §10, D154) adds the goal links (`goalId`), and this plan adds:
1. the plugin tables (§6.1);
2. one `plugin_install` row: `app.beaesthetic.workout`, `source = BUILT_IN`, enabled, attached to the migrated program goal;
3. `task.pluginId` on the workout task.

**One migration, one release.** Ship it together with the goal migration, so the user's phone takes one backup and one migration.

**Tests** (added to the existing ones):
- a migration test that opens a v10 database full of real-looking workout data and checks every row after migrating;
- the backup round trip with the new tables;
- turn the workout plugin off → Today has no workout card and nothing is deleted → turn it on → everything is back;
- `./gradlew check` and every existing workout test still pass.

**On your phone:**
1. Back up.
2. Restore that backup on an emulator, install the new build there, and compare.
3. Then install on the Poco.
4. Keep the backup.

A fresh install shows the workout plugin as available and off (D168).

---

## 13. Build order: small vertical slices

Each slice ends with what works, what's next, and your feedback. Sizes are S, M or L, for ordering only.

| # | Slice | Done means | Size |
|---|---|---|---|
| **S0** | **Spike: the riskiest part, on the Poco.** A throwaway branch. Zipline in an isolated-process service; a tiny reading-log plugin (Kotlin/JS) with one daily task, an amount metric, a goal section (level ①) and a small canvas chart (level ②); installed from a file signed by a .NET test tool. | Measured on the Poco: cold and warm open time of the section, host-call round trip (p50 and p95), extra memory, APK growth. Targets: cold ≤ 400 ms, warm ≤ 120 ms, round trip p95 ≤ 5 ms, ≤ 30 MB, ≤ 3 MB. These are targets, not facts. Proven: the isolated process can't open a socket or the database file; killing it shows the calm message and the next tap restarts it; HyperOS behavior with the screen off, a recents swipe and battery saver is written down; a .NET-signed test vector verifies on Android. The same open time and memory measured for JavaScriptSandbox, for comparison. The engine choice recorded in `DECISIONS.md`. Also: the written question sent to Bazaar support. | M |
| **S1** | **The contract:** `:plugin-api` (JVM + JS): manifest, UI tree, events, services, validation; a JSON schema for the manifest. | Both targets compile; round-trip and validation tests pass; the first `CONTRACT.md`. | M |
| — | _(v1.0's goal data model, from SPEC §9, comes before S2; it isn't part of this plan.)_ | | |
| **S2** | **Host core:** `:plugin-host` registry, permission broker, storage, quotas; the plugin tables; backup and restore. | Migration test; backup round trip with plugin tables; quota and scoping tests (a plugin can't read another's documents). | M |
| **S3** | **The renderer, level ①:** `:design` split out of `:app`; Compose rendering of the level ① components; events; host-owned back; `savedState`. | A fake in-process plugin renders a Today card, a goal section and a screen, correct in fa-RTL and en-LTR, light and dark; back works on Android 16. | L |
| **S4** | **The runtime, for real:** the isolated-process service from the spike made solid: lazy start, idle stop, limits, crash counting, the `App.onCreate` guard, the shared process on Android 8–9. | Chaos plugins (an endless loop, a memory bomb, a crasher) leave the app usable; the network test fails as it should; works on an Android 8 emulator. | M |
| **S5** | **The workout as a built-in plugin + the migration**, together with the goal migration (D154). | §12's tests pass; the user's restored backup checked on an emulator, then on the Poco. | L |
| | **v1.0 ships here** (the brief's timing). | | |
| **S6** | **The sample plugin end to end:** the reading log installed from a signed file (Developer mode). | Works offline on the Poco; ticking its task never starts it; it's in the backup; a restore on another device brings it back offline; remove keeps the data; delete data asks first. | M |
| **S7** | **Level ②:** free layout, canvas, gestures, motion. | A custom sample screen; the harness checks L5, L6, T2, A1–A3 automatically. | L |
| **S8** | **SDK, harness, starter, docs** (English and Persian). | A fresh starter builds an installable plugin in under 10 minutes; `pluginCheck` catches seeded mistakes (a missing string, a Latin digit in Persian, a missing label, `eval`). | L |
| **S9** | **`:store`:** the WebView, the bridge, downloads, updates, revocations; `checkNetworkFence` replaces the manifest check (D167). | Installs from a staging backend; the fence fails a test module that imports OkHttp; the WebView can't leave the store origin; the bridge ignores other origins. | M |
| **S10** | **The .NET backend MVP:** catalog, submissions, checks, review console, signing tool, revocations. | A plugin goes from upload to review to signed to installed in the app. | L |
| **S11** | **Play readiness:** the `play` flavor, its channel and copy, and the app-wide Play checks (§11.2). | A build that passes your own checklist for Play. | S |
| **S12** | **A pilot:** one or two invited developers. | One outside plugin approved and installed; the docs fixed from their questions. | — |

---

## 14. Open questions

### Round 1: answered 2026-10-22 (the recommended option each time; D177–D181)

1. **The engine inside the isolated process.**
   (a) **Zipline (QuickJS)** (recommended: a typed Kotlin contract, works on every phone from Android 8, maintained by Block) · (b) JavaScriptSandbox (Google keeps the sandbox up to date, but it needs WebView 104+ and newer versions for some features, and many phones in Iran don't update WebView) · (c) Zipline in the app process (the fastest to build, but its README says it isn't for untrusted code).
2. **Level ②, the plugin's own UI.**
   (a) **A host-drawn free layout first; a WebView only later, if a real plugin needs it** (recommended: the rules are enforced by code and there's no network to fence) · (b) a sandboxed WebView now (the most freedom, rules checked by review) · (c) both now.
3. **The language for plugin authors.**
   (a) **Kotlin only, through the SDK** (recommended: the same language as the host and the contract, one SDK to maintain) · (b) Kotlin and TypeScript (more authors, but a second SDK and a JSON bridge).
4. **The workout plugin's code.**
   (a) **A built-in native plugin; its data stays in its tables** (recommended: no data moves and the screens stay as they are) · (b) port it to the script runtime.
5. **Removing a plugin.**
   (a) **Hide it and keep its data; deleting data is a separate step** (recommended: never lose data) · (b) delete the data after a confirmation.

### Round 2: after v1.0, before the store (S9)

6. **Bazaar.** (a) **Email Bazaar support now: "may an app download reviewed, signed JavaScript add-ons that run in a sandbox?"** (recommended: one email removes the biggest unknown) · (b) ship and see.
7. **The signing algorithm.** (a) **ECDSA P-256** (recommended: built into Android 8+ and .NET) · (b) Ed25519 (needs a library below Android 13).
8. **Who can submit.** (a) **Invite-only at first** (recommended: a review load you can carry) · (b) open sign-up.
9. **Hosting for Play users abroad.** (a) **The Iran host plus a mirror for the signed packages** (recommended: signed packages are safe anywhere) · (b) the Iran host only.
10. **Processes.** (a) **One isolated process per running plugin on Android 10+** (recommended: a crash or leak stays in one plugin; decide with the spike's memory numbers) · (b) one shared process for all.

---

## 15. Decisions

**Recorded 2026-10-22:** D177 (refines D166: Zipline inside an isolated process), D178 (refines D172: level ② is a host-drawn free layout), D179 (Kotlin only), D180 (the workout as a built-in native plugin; refines D163 and D168), D181 (removing a plugin keeps its data).

**Still proposed.** They go into `ARCHITECTURE.md` in P0, and into `DECISIONS.md` once confirmed:
- **D171, refined:** the store bridge uses `addWebMessageListener` limited to the exact store origin. HTTPS only, never `addJavascriptInterface`.
- **D173, refined:** Play's interpreter exception is confirmed, with the duty to review plugins against Play's policies. Bazaar has no written exception: ask in writing (round 2, question 6); until Bazaar answers, the Bazaar build ships built-in plugins only.
- **New:** plugins never run in the background. Daily actions are host tasks. Today cards are published snapshots with live host nodes.
- **New:** the host formats every number and date. Plugins get the app's language, direction and digits from the host, never from the phone. (D178 already says this for level ② screens.)
- **New:** packages are signed over a list of file hashes; two keys are pinned; there is a signed revocation list; installed packages are included in backups. The algorithm is round 2, question 7.

## 16. Not in this plan

- Paid plugins (D169). When they come: Bazaar's billing in the Bazaar build and Play Billing in the Play build (research §4.1).
- Plugin-to-plugin APIs, background plugins, plugins that add notifications of their own.
- Level ③ (WebView) and TypeScript authoring, unless round 1 says otherwise.
- The Play release itself: its permissions, the Data safety form and the privacy policy are app-wide work (research §8).
