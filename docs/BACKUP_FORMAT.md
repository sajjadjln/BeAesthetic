# Backup format

_Format version 1, written by app 0.6.0 (database schema 6). Decisions: D39, D132–D135. Code: `app/.../backup/BackupArchive.kt`._

A backup is one zip file. It is meant to be read by the app on any phone, and later by other tools (a .NET backend, a script on the PC).

## File name

`beaesthetic-1405-07-22-214201.zip`: the Jalali date (year, month, day) and the local time to the second, all ASCII digits. Names sort by time. The app keeps the newest 8 in the folder and never touches other files there. A file ending in `.zip.partial` is a backup that was cut off while being written: it is never valid, and the next backup deletes it.

## Inside the zip

| Entry | What it is |
|---|---|
| `manifest.json` | What the backup holds. Read first, before anything changes. |
| `data.json` | Every table, row by row. **The source of truth for restore.** |
| `photos/…` | The app's photos, same paths as in the app's `files/photos/` folder: `<checkIn>-<pose>.jpg` (slice 7a, schema 7). |
| `db/beaesthetic.db` | A raw copy of the SQLite database, checkpointed first. An emergency fallback only; restore never reads it. |

## manifest.json

```json
{
  "formatVersion": 1,
  "appVersion": "0.6.0",
  "dbSchemaVersion": 6,
  "createdAt": "2026-10-14T18:12:01.461Z",
  "reason": "MANUAL",
  "counts": { "task": 21, "completion": 3, "day": 4, "...": 0 },
  "photos": { "0-front.jpg": 812345 }
}
```

- `formatVersion`: the layout of the zip and of `data.json`. A reader refuses a version it doesn't know.
- `dbSchemaVersion`: the Room schema the rows come from (`app/schemas/…/<n>.json` describes each one).
- `reason`: `MANUAL`, `AUTO` or `BEFORE_RESTORE`.
- `counts`: rows per table in `data.json`. `photos`: path under `photos/` → size in bytes. Both are checked after writing.

## data.json

```json
{
  "formatVersion": 1,
  "dbSchemaVersion": 6,
  "tables": {
    "task": [ { "id": 1, "title": "مسواک", "icon": "TOOTH", "part": "MORNING", "repeat": "DAILY", "startDate": "2026-10-10", "isImportant": 0, "...": null } ],
    "completion": [ { "taskId": 1, "date": "2026-10-10", "completedAt": 1791660000000 } ]
  }
}
```

Each table is an array of rows; each row maps column names to values exactly as SQLite stores them:

- **Dates** (`LocalDate`) are ISO strings, `"2026-10-10"`. Gregorian; Jalali is presentation only.
- **Instants** are epoch milliseconds (numbers).
- **Times of day** are minutes after midnight.
- **Booleans** are `0` or `1`.
- **Enums** are their names (`"MORNING"`, `"ANYTIME"`).
- **Weekday sets** are bit sets, Saturday = bit 0 … Friday = bit 6.
- Missing values are `null`. There are no binary columns.

The tables and columns are the ones in `ARCHITECTURE.md` §8. Two things are left out on purpose: the `backup_record` table, and the phone's own settings `backupTreeUri`, `lastBackupAt` and `autoBackup`. Those describe this phone's backup folder, so a restore keeps the phone's own values (D132).

## Restore rules

1. Read `manifest.json`. Refuse a file without one, a newer `formatVersion`, or a newer `dbSchemaVersion` than the app has («اول اپ رو به‌روز کن»).
2. Make a safety backup of the current data: into the backup folder, or, with no usable folder, into the app's private storage (the newest two are kept). If the safety backup fails, stop.
3. In one transaction: empty every table (except `backup_record`), insert every row from `data.json`, then put back this phone's backup settings. Any error rolls the whole thing back.
4. Unpack the photos to a side folder first, and swap it in only after the data is in.
5. Plan reminders again.

**Old backups restore into newer apps** because schema changes stay additive: new tables, and new columns that have defaults. A table the backup doesn't have ends up empty; a column it doesn't have takes its default. A change that renames or drops a column must add a mapping step here and raise `formatVersion`.
