"""汇总本次全量测试的 JUnit XML 结果（只统计 mtime 晚于起跑时间的文件）。"""
import collections
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = sys.argv[1]
START = int(open(sys.argv[2]).read().strip())
OUT = sys.argv[3]

SKIP_DIRS = {"external", ".git", "node_modules", ".gradle", ".claude", "intellij-ide", "deveco"}

per_task = collections.OrderedDict()
failures = []
stale = 0

for dirpath, dirnames, filenames in os.walk(ROOT):
    dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
    if "test-results" not in dirpath.replace("\\", "/").split("/"):
        continue
    for fn in filenames:
        if not (fn.startswith("TEST-") and fn.endswith(".xml")):
            continue
        path = os.path.join(dirpath, fn)
        if os.path.getmtime(path) < START:
            stale += 1
            continue
        rel = os.path.relpath(path, ROOT).replace("\\", "/")
        m = re.match(r"(.*)/build/test-results/([^/]+)/", rel)
        key = f":{m.group(1).replace('/', ':')}:{m.group(2)}" if m else rel
        try:
            suite = ET.parse(path).getroot()
        except ET.ParseError as e:
            failures.append({"task": key, "class": fn, "name": "<XML 解析失败>", "kind": "parse", "msg": str(e)})
            continue
        s = per_task.setdefault(key, collections.Counter())
        for tc in suite.iter("testcase"):
            s["tests"] += 1
            status = "passed"
            for kind in ("failure", "error"):
                el = tc.find(kind)
                if el is not None:
                    status = kind
                    msg = (el.get("message") or el.text or "").strip()
                    first = msg.splitlines()[0][:300] if msg else ""
                    failures.append({
                        "task": key,
                        "class": tc.get("classname"),
                        "name": tc.get("name"),
                        "kind": kind,
                        "type": el.get("type"),
                        "msg": first,
                    })
                    break
            if status == "passed" and tc.find("skipped") is not None:
                status = "skipped"
            s[status] += 1

with open(OUT, "w", encoding="utf-8") as f:
    json.dump({"per_task": per_task, "failures": failures, "stale_xml": stale}, f, ensure_ascii=False, indent=1)

tot = collections.Counter()
for k, c in per_task.items():
    tot.update(c)
print(f"stale xml ignored: {stale}")
print(f"TOTAL tests={tot['tests']} passed={tot['passed']} failed={tot['failure']} error={tot['error']} skipped={tot['skipped']}")
print(f"{'task':70} {'tests':>6} {'pass':>6} {'fail':>5} {'err':>5} {'skip':>5}")
for k, c in sorted(per_task.items()):
    print(f"{k:70} {c['tests']:>6} {c['passed']:>6} {c['failure']:>5} {c['error']:>5} {c['skipped']:>5}")
