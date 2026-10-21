# Prompt: plugin platform research

_For a separate research session (Opus, max effort). Updated 2026-10-22 for D172–D176. Copy everything inside the block._

```text
You are a senior Android architect. Research whether a plugin-based Android app is achievable for my project, the way extensions work in VS Code. If it is, write a full plan for building it. Verify every technical claim against current official sources (Android developer docs, AOSP source, library repos, Google Play and Cafe Bazaar policy pages) and cite them. Mark anything you couldn't verify.

## My project
- A personal Android app ("پیکر", repo: BeAesthetic). Kotlin, Jetpack Compose, Room, a pure-Kotlin :domain module. If you can access the repo, read CLAUDE.md, docs/DECISIONS.md (D148–D176), docs/SPEC.md and docs/tasks/plugin-platform.md first, and check app/build.gradle.kts for minSdk/targetSdk.
- The app is a to-do list + goals (set a goal, break it down, monitor progress).
- Languages: the whole app is in English and Persian.
  - Persian: RTL, Persian digits, Jalali dates first.
  - English: LTR, Latin digits, Gregorian dates first.
  - The language follows the phone, with a switch in Settings.
- A "plugin" is a template for one goal type, with its own screens on top of the app. The first plugin is the existing workout feature: plan editor, workout session, ladders, records, strength tests, monthly check-in with a camera overlay. It's built in, off by default, and the user switches it on.
- Third-party developers will write plugins against our published contract and guidelines. They submit them to an online store I build myself (.NET backend). I review each one and list only approved ones. Plugins are free at first. The store screen in the app is a WebView limited to the store domain. Installing goes through one native bridge call, with a signature check.
- Plugin UI has two levels:
  1. Host components (tiles, cards, bands, steppers, charts, sheets) as the easy default.
  2. The plugin's OWN UI, drawn freely, as long as it meets a minimum standard written into the contract:
     - it follows the app's current language, layout direction and digits;
     - all its text lives in string files, it ships at least one language, and the store shows its languages;
     - it uses the host's theme tokens and light/dark, never hard-coded colors;
     - system back behaviour works;
     - accessibility basics;
     - no network;
     - data only through the host API;
     - the product rules: no red for misses, no streaks that reset, no guilt wording.
- Distribution: Cafe Bazaar and Google Play. Both must be considered, and one design should work on both if possible.
- Hard constraints:
  - no Google Play Services or Firebase inside the app;
  - no analytics;
  - the app works offline;
  - only the :store module may use the network, enforced by a build check;
  - plugin data must be in the app's own backup and restore;
  - plugins must not be able to read the user's other data;
  - target phone: Poco X4 5G on HyperOS (Android 14 based), but any Android phone must work.
- Current decisions:
  - D166: plugins run as scripts in a sandboxed runtime (e.g. Zipline/QuickJS).
  - D173: Google Play bans downloaded native code but allows interpreted code, so plugins stay scripts.
  Verify this reasoning. Also answer: could plugin code STAY NATIVE (Kotlin/JVM bytecode) in any form that both stores allow? Challenge the decisions if the evidence says so.

## Research questions
1. How VS Code extensions work: the extension host process, the manifest and contribution points, activation events, API versioning (engines), localization of extensions, webviews for custom UI, sandboxing limits, the marketplace and publisher verification. What maps to Android and what doesn't.
2. Prior art on Android: apps with third-party plugins (Obsidian mobile, Tasker plugins, Termux add-ons, launchers, keyboards, Muzei sources, Firefox/Kiwi extensions, others you find). How do they load, isolate, localize and draw plugin UI, and how do they stay within Google Play policy?
3. Every way to run plugin code, compared:
   a. a separate APK per plugin with an SDK (AIDL/Binder, ContentProvider, bound services, Activities);
   b. native code loaded into the app (DexClassLoader / InMemoryDexClassLoader) in the main process;
   c. native code loaded into an isolated process (android:isolatedProcess services, Binder contract, limits, memory and startup cost);
   d. a script runtime (Zipline/QuickJS, Hermes, J2V8, Kotlin/JS, Kotlin/Wasm);
   e. purely declarative plugins (JSON/DSL, no code).
   For each: security and isolation, whether it can stay native Kotlin, UI options, localization and RTL/LTR, data access and backup, performance on a mid-range phone, size, developer experience, API versioning, review and signing, maintenance burden, and whether it's allowed on Google Play and on Cafe Bazaar (quote the policy text).
4. Plugin UI options: native Compose inside the app, a separate Activity from a plugin APK, SurfaceControlViewHost (embedded UI from another process), a host-drawn UI tree (server-driven UI), RemoteViews, a sandboxed WebView with our design kit (like VS Code webviews). For each:
   - freedom for the plugin author;
   - how it handles both languages and both directions;
   - how the minimum UI standard is enforced (by code, by lint/tests, or by review);
   - store-policy risk.
5. Platform limits: Android 14+ rules for dynamic code loading, Android version requirements of each option, per-app language support (AppCompat), what HyperOS/MIUI does to extra processes and services.
6. Security: code signing (store key pinned in the app), a per-plugin permission model, preventing data exfiltration, resource limits, crash isolation, safe updates and data migrations, uninstall without data loss.

## Deliverables
1. A research report:
   - findings per question;
   - a comparison matrix of options a–e, including Play-allowed and Bazaar-allowed columns;
   - the risks;
   - a clear verdict: is a VS Code-style plugin platform achievable for this app, and can plugin code stay native under both stores?
   Recommend ONE architecture for my constraints and explain why.
2. If achievable, a plan:
   - architecture: modules, processes, the contract, extension points, both UI levels, localization (two languages, both directions), storage and backup, install/update/remove, signing;
   - a draft plugin contract: manifest fields (including supported languages), the host API as Kotlin interfaces, permissions, versioning, and the minimum UI standard written as testable rules;
   - the developer kit: SDK, starter project, test harness (both languages, both directions, light/dark), guidelines, review checklist;
   - the store flow, app side and .NET backend side, for both Cafe Bazaar and Google Play builds;
   - how the existing workout feature becomes the first plugin without losing my data;
   - build order as small vertical slices, each with what "done" means, starting with a spike that proves the riskiest part on a real phone;
   - open questions for me, 3–5 at a time, multiple choice, with your recommended option first and a one-line reason.
3. Write in plain English, short sentences. If you can write files, save the report as docs/research/plugin-platform-research.md and the plan as docs/tasks/plugin-platform-plan.md, and don't change any code.
```
