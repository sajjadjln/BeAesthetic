# Task: the plugin platform

_Written 2026-10-22 as the brief for its own session. Decisions D163–D171 in `DECISIONS.md`._

Read `CLAUDE.md`, `docs/TASKS.md`, `docs/DECISIONS.md` (D148–D171) and `docs/SPEC.md` v1.0 first. Follow the user's process: this task starts in **Phase 4 (architecture)**. Nothing is built until the user approves the architecture and the contract. Then it's built in small slices, with feedback after each one.

## 1. What a plugin is

- A **plugin is a template for one goal, with its own screens on top of the app** (D163). Example: the workout plugin is the plan editor, the session, ladders, records, strength tests and the check-in with the ghost camera, all attached to a fitness goal.
- The core app (to-do, goals, the wall, reviews, reminders, backup) works with no plugin at all.
- Plugins are **script code in a sandboxed runtime inside the app** (D166). A plugin can only call the host's plugin API. It draws with the host's components or with its own UI that meets the contract's minimum standard (D172). It can't touch the network, files, other plugins or the user's other data.
- **Stores:** Cafe Bazaar and possibly Google Play (D173). Play bans downloaded native code but allows interpreted code, so plugins stay scripts. Every design choice must pass both stores' policies.
- The **workout plugin is built into the APK, off by default**, and the user switches it on in the app (D168). The migration switches it on for the user's existing program goal.
- An **online store** that the user builds (.NET, hosted in Iran, D170) lists reviewed plugins. The store screen in the app is a **WebView** limited to the store's domain (D171). It's the only online part of the app (D164). Only the `:store` module may use the network (D167).
- **Timing (accepted 2026-10-22):** v1.0 sets the contract and ships the workout as the first, built-in plugin (P0–P3). The store, outside plugins and the backend come after v1.0 (P4–P6).
- **Anyone can write a plugin** against published guidelines, standards and contracts. It's listed only after review and approval (D165). **Free** at first (D169).

## 2. Constraints that don't move

- No shame mechanics: nothing red, no streaks that reset, no lists of misses, no guilt wording. These go into the components and the review checklist.
- Persian only, RTL, Persian digits, Jalali first. Plugin strings come with the plugin, in Persian.
- Offline first: an installed plugin works with no connection, forever.
- Never lose data: plugin data is in the app's database and in every backup and restore. Uninstalling a plugin asks before deleting its data, and offers to keep it.
- No Google Play Services, Firebase, analytics or crash SDKs. The runtime library must pass the banned-dependency check.
- Today first, one tap to log: a plugin's daily action is a normal task tile.

## 3. Work, in order

### P0. Architecture (approve before any code)
1. A plugin section in `docs/ARCHITECTURE.md`: modules, the runtime, the API boundary, storage, backup, installing, updating, removing, versioning.
2. `docs/plugins/CONTRACT.md`: the plugin manifest, the host API, extension points and permissions (P1).
3. `docs/plugins/GUIDELINES.md`: design and copy rules, coding standards, size and performance limits, the review checklist (P6).
4. A runtime spike (P2) to pick the engine, with the reasons written in `DECISIONS.md`.
5. Ask the user the open questions in §5 (3–5 at a time, the recommended option first).

### P1. The contract (`:plugin-api`, pure Kotlin)
- **Manifest:** id (reverse domain), version (semver), `minHostApi`, Persian name and description, icon, author, the goal types it fits, permissions, size.
- **Extension points** (where a plugin can appear):
  - steps added to the goal flow (for example «برنامه‌ی تمرین» after step ④);
  - the goal's daily actions (become normal tasks);
  - a card or tile on Today (for example «تمرین امروز»);
  - sections on the goal's page;
  - full-screen flows (for example the workout session);
  - a part of the monthly chapter review (for example the check-in);
  - lead and outcome metrics fed into the goal (sessions, kg, reps);
  - its own settings screen.
- **Host API** (what a plugin can call): read the goal it's attached to; create and update its own tasks; tick or read its tasks' completions; plugin-scoped storage; metrics; schedule reminders through the host; the camera and photo storage (permission); haptics; Jalali and day-rollover helpers. Nothing else.
- **Permissions:** declared in the manifest, shown before install, enforced by the host (camera, photos, reminders, metrics).
- **Versioning:** `hostApi` level; the host refuses a plugin that needs a newer one and says why.

### P2. The runtime (`:plugin-runtime`)
- Pick the engine in a spike. Candidates: **Zipline** (Cash App; Kotlin/JS on QuickJS; Kotlin interfaces across the bridge; signed manifests; can load from local files), a plain QuickJS binding, or a JSON UI schema with no code at all. Judge them on: APK size, startup time on the Poco, how contracts are expressed, sandboxing, code signing, maintenance, no Google dependencies.
- **Two UI levels (D172).**
  - ① **Host components:** a plugin returns a UI tree; the host draws it with its own Compose components (tile, card, chip, band, path, stepper, counter, chart, list row, sheet, button, seek bar, photo slot). Tokens and accents come from `DESIGN.md`. The rules hold by construction.
  - ② **The plugin's own UI:** likely a sandboxed WebView with the app's design kit (CSS, Estedad, theme tokens, light/dark), with no network and a message bridge to the host API only. The minimum standard is in the contract as testable rules: RTL and Persian digits, host theme tokens (no hard-coded colors), system back, accessibility basics, data only through the host API, and the product rules (checked in review).
- **Storage:** a plugin-scoped document store in Room (plugin id, collection, key, JSON, schema version), with the plugin's own migrations run by the host. Included in `data.json` (`BACKUP_FORMAT.md`) and in restore.
- Limits: CPU time per call, memory, storage size. A plugin that crashes or times out is paused with a calm message and never takes the app down.

### P3. The workout as the first plugin
- **Open (§5):** a native first-party plugin that implements the same contract in Kotlin, or a port to the script runtime.
- Built into the APK; listed as available, off by default (D168). Switching it on attaches it to a goal (the fitness template pre-fill from SPEC §5.11).
- Migration: the user's current workout plan, sessions, set logs, ladders, metrics, photos and check-ins move under the plugin, switched on for the migrated program goal. Tested like every other migration, with a backup first.
- `./gradlew check` and every workout test still pass.

### P4. The store in the app (`:store`)
- The only module with network access. A build check fails if any other module uses network APIs or network libraries (replacing D60's manifest check, D167). Requests go only to the store domain.
- **The WebView** shows the store's pages (D171):
  - it loads only the store domain; any other link opens outside the app or not at all;
  - no file access, no content access;
  - one bridge method: `install(pluginId, version)`.
- **Installing:** the native side downloads the package, checks the store's signature (the store's public key is pinned in the app), checks `minHostApi` and size, shows the name, author, version and permissions, and saves it only on «نصب».
- **Updates:** the app checks for updates only while the store screen is open, then asks before updating. A plugin's data migrates with the host's migration runner.
- **Sideloading for developers:** debug builds can install a plugin file from the file picker, still signature-checked against a developer key.
- The store never receives the user's data. Requests carry only plugin ids, versions and the host API level.

### P5. The store backend (separate repository, .NET, hosted in Iran)
- Catalog: plugins, versions, Persian descriptions, screenshots, ratings later.
- Developer accounts and submissions (upload a package with its manifest and source).
- The review queue for the user: approve or send back with notes, following the checklist.
- Signing approved versions with the store's private key (kept off the web server).
- The web pages shown in the WebView: Persian, RTL, the app's look, light and dark.
- No analytics on users. Download counts only.

### P6. Developer docs and SDK
- A Persian and English guide: how a plugin works, the contract, the components, the permissions, the design and copy rules, coding standards, testing.
- A starter project and a local test harness (run a plugin against a fake host on a laptop).
- The **review checklist**: no red, no streaks, no guilt copy, no lists of misses; Persian strings; only host components; declared permissions match use; data migrations are present and tested; size and performance limits; no hidden network or tracking.
- A packaging tool that builds the package and its manifest for submission.

## 4. What done looks like

- **For the first session:** P0 is written and approved by the user, and the runtime is chosen with a spike that runs a tiny sample plugin (for example a reading log: one daily task, an amount metric, one goal-page section) on the Poco.
- **For the whole task:** the workout runs as a plugin with the user's data intact; a second, sample plugin installs from a signed file; the store screen lists and installs plugins from the .NET backend; the developer guide and checklist are published.

## 5. Open questions to ask the user (with my recommendations)

1. **The workout plugin's code:** (a) **native Kotlin implementing the same contract first (recommended:** keeps the working screens and the user's daily use safe; the sample plugin proves the runtime) · (b) port it to the script runtime now.
2. **Runtime engine:** decide after the spike; **Zipline is the first candidate** (Kotlin contracts across the bridge, signed code, QuickJS).
3. **What plugins are written in:** (a) **Kotlin (compiled for the runtime; recommended:** the same language as the contract and the host) · (b) JavaScript or TypeScript (more developers know it).
4. **Removing a plugin:** (a) **keep its data hidden, so switching it back on restores everything (recommended:** never lose data) · (b) delete the data after a confirmation.
