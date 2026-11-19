# Sample backups (B3)

One backup per shipped backup format and database schema. `BackupSamplesTest` restores each into the newest schema and checks every row. It fails when a new format or schema ships without a sample here.

| File | Format | Schema | Where it comes from |
|---|---|---|---|
| `lantern-0.9.4-schema2.zip` | 1 | 2 | **Real.** Made by the signed 0.9.4 release build on the emulator on 2026-11-05 (Settings → Backup → «همین الان پشتیبان بگیر»). One task with an alarm, one tick, the wizard and the parts layout. Same writer as Bazaar 0.9.5–0.9.7 (those add an optional `crashes/` folder, covered by `BackupRoundTripTest`). |
| `lantern-0.1.0-schema1.zip` | 1 | 1 | **Derived.** No 0.1.0 install is left to make one. 0.1.0 used the same writer; schema 1 differs only by the two settings columns that 1 → 2 added. So it is the 0.9.4 sample with `settings.character` and `settings.todayLayout` removed and `appVersion` 0.1.0, schema 1 (`scripts/backup-sample-schema1.py`). |

| `lantern-goals-schema3.zip` | 1 | 3 | **Development fixture, 2026-11-18.** Schema-2 sample extended with a goal, required Persian why, milestone, linked task and finish date for the 1.0.1 schema. The current writer and old backups are covered by round-trip tests. |

«پیکر» (v0.x, `app.beaesthetic`) backups are not here: they come from a different database (its schemas 1–10) and restoring them into Lantern isn't supported (D213).

**When a schema or format changes:** make a backup with the new build, add it here with a row above, and add it to `samples` in the test.
