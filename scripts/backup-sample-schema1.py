#!/usr/bin/env python3
# Usage: python3 scripts/backup-sample-schema1.py <schema-2 backup.zip> <out.zip>
# Builds the schema-1 sample from the real 0.9.4 (schema 2) backup: the 0.1.0 writer was the same code, and
# schema 1 differs only by settings.character and settings.todayLayout (added by the 1 -> 2 auto-migration).
import json, sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as z:
    manifest = json.loads(z.read('manifest.json'))
    data = json.loads(z.read('data.json'))
for row in data['tables']['settings']:
    row.pop('character'); row.pop('todayLayout')
data['dbSchemaVersion'] = 1
manifest['dbSchemaVersion'] = 1
manifest['appVersion'] = '0.1.0'
with zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('manifest.json', json.dumps(manifest, separators=(',', ':'), ensure_ascii=False))
    z.writestr('data.json', json.dumps(data, separators=(',', ':'), ensure_ascii=False))
