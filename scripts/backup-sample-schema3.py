#!/usr/bin/env python3
"""Derives the schema-3 sample (goals, D223) from the real 0.9.4 schema-2 sample: adds a goal with its why,
its months and one weekly action (a task with goalId) ticked once. Replace with a real backup from a goals build."""
import json, sys, zipfile
src, out = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as z:
    manifest = json.loads(z.read("manifest.json")); data = json.loads(z.read("data.json")); db = z.read("db/lantern.db")
t = data["tables"]
for row in t["task"]: row["goalId"] = None
t["task"].append({**t["task"][0], "id": 2, "title": "۲۵ دقیقه تمرین", "icon": "CHECK", "part": "EVENING", "repeat": "WEEKDAYS",
                  "weekdays": 21, "alarmTime": None, "alarmWeekdays": 0, "alarmEnabled": 0, "sortOrder": 1000, "goalId": 1})
t["completion"].append({"taskId": 2, "date": "2026-11-07", "completedAt": 1794050000000})
t["goal"] = [
    {"id": 1, "title": "آیلتس ۷ بگیرم", "state": "ACTIVE", "startDate": "2026-11-05", "endDate": "2027-03-20", "triedBefore": 1,
     "minutesPerWeek": 60, "triedWhat": "یه ماه کلاس", "stopReason": "BORED", "flexDays": 1, "why": "چون می‌خوام یه زندگی تازه بسازم",
     "whyAt": 1793852055911, "quietOfferedOn": None, "sortOrder": 1, "createdAt": 1793852055911, "reachedAt": None, "setAsideAt": None},
    {"id": 2, "title": "گیتار", "state": "IDEA", "startDate": "2026-11-05", "endDate": None, "triedBefore": None, "minutesPerWeek": None,
     "triedWhat": None, "stopReason": None, "flexDays": 1, "why": None, "whyAt": None, "quietOfferedOn": None, "sortOrder": 2,
     "createdAt": 1793852055911, "reachedAt": None, "setAsideAt": None},
]
t["goal_month"] = [{"goalId": 1, "monthIndex": 0, "milestone": "لغت"}, {"goalId": 1, "monthIndex": 1, "milestone": None}]
data["dbSchemaVersion"] = manifest["dbSchemaVersion"] = 3
manifest["appVersion"] = "1.0.0-goals"
manifest["counts"] = {k: len(v) for k, v in sorted(t.items())}
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, separators=(",", ":")))
    z.writestr("data.json", json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    z.writestr("db/lantern.db", db)
