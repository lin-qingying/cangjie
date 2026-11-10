#!/usr/bin/env bash
set -u
cd /tmp
curl -sSL -m 40 "https://api.gitcode.com/api/v5/repos/Cangjie/nightly_build/releases?per_page=3" -o rel.json
echo "json bytes: $(wc -c < rel.json)"
python3 - <<'PY'
import json
rels = json.load(open('/tmp/rel.json'))
for r in rels:
    print("release:", r.get('tag_name'), "| published:", r.get('published_at') or r.get('created_at'))
    for a in r.get('assets') or []:
        n = a.get('name'); u = a.get('browser_download_url') or a.get('download_url')
        print("   asset: %-52s %s" % (n, u))
PY
