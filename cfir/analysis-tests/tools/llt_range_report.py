#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""聚焦「诊断名一致、范围不同」的失败，按发生位移的诊断名聚类。"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter, defaultdict

sys.path.insert(0, "cfir/analysis-tests/tools")
from llt_console_report import parse, markers, fixture_of  # noqa: E402

MARKER_RE = re.compile(r"<!([^!<>]+)!>")


def diff_lines(exp, act):
    """返回 (期望行, 实际行) 的差异对；按行号朴素对齐。"""
    out = []
    n = max(len(exp), len(act))
    for i in range(n):
        e = exp[i].strip() if i < len(exp) else ""
        a = act[i].strip() if i < len(act) else ""
        if e != a:
            out.append((e, a))
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("log")
    ap.add_argument("--top", type=int, default=30)
    ap.add_argument("--diag", help="只看该诊断名的位移样例")
    ap.add_argument("--samples", type=int, default=3)
    args = ap.parse_args()

    records = parse(args.log)
    groups = defaultdict(list)
    for r in records:
        if not r["expected"] or not r["actual"]:
            continue
        if markers(r["expected"]) != markers(r["actual"]):
            continue
        pairs = diff_lines(r["expected"], r["actual"])
        names = set()
        for e, a in pairs:
            for m in MARKER_RE.findall(e) + MARKER_RE.findall(a):
                for nm in m.split(","):
                    if nm.strip():
                        names.add(nm.strip())
        key = ",".join(sorted(names)) or "-"
        groups[key].append((fixture_of(r), pairs))

    print(f"range-only failures: {sum(len(v) for v in groups.values())} testcases")
    for key, items in sorted(groups.items(), key=lambda kv: -len(kv[1]))[: args.top]:
        fixtures = sorted({f for f, _ in items})
        print(f"\n[{len(fixtures)} fixtures] {key}")
        for f in fixtures[:6]:
            print(f"      {f}")

    if args.diag:
        print(f"\n===== {args.diag} 位移样例 =====")
        shown = 0
        for key, items in groups.items():
            if args.diag not in key.split(","):
                continue
            for f, pairs in items:
                print(f"\n--- {f}")
                for e, a in pairs[:6]:
                    print(f"  EXP: {e}")
                    print(f"  ACT: {a}")
                shown += 1
                if shown >= args.samples:
                    return 0
    return 0


if __name__ == "__main__":
    sys.exit(main())
