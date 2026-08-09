#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从 Gradle 控制台全量日志聚合 LLT 失败。

Windows 上生成测试类名过长会导致 XML/HTML 报告写入不全，
因此改为解析 `--console=plain` + `exceptionFormat=FULL` 的控制台日志。

日志中每个失败形如：

    Suite > Group > testX() FAILED
        org.opentest4j.AssertionFailedError: Actual data differs from file content: a.cj
        =====预期======
        <带 <!DIAG!> 标记的源码>
        =====得到======
        <带 <!DIAG!> 标记的源码>
            at ...

用法：
    python cfir/analysis-tests/tools/llt_console_report.py <log> [--top N] [--diag NAME] [--cluster]
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter, defaultdict

FAILED_RE = re.compile(r"^(CfirAnalysis\w*TestGenerated) > (.+?) FAILED\s*$")
HEADER_RE = re.compile(r"^\s+(\S+(?:Error|Exception)):\s*(.*)$")
SPLIT_RE = re.compile(r"^\s*={3,}[^\n=]*={3,}\s*$")
MARKER_RE = re.compile(r"<!([^!<>]+)!>")
AT_RE = re.compile(r"^\s+at [\w.$]+\(")


def parse(path: str):
    """产出失败记录列表。"""
    records = []
    cur = None
    part = None  # None / 'expected' / 'actual'
    with open(path, encoding="utf-8", errors="replace") as fh:
        for line in fh:
            m = FAILED_RE.match(line)
            if m:
                cur = {
                    "suite": m.group(1),
                    "test": m.group(2).strip(),
                    "kind": "",
                    "detail": "",
                    "expected": [],
                    "actual": [],
                }
                records.append(cur)
                part = None
                continue
            if cur is None:
                continue
            if AT_RE.match(line):
                part = None
                cur = None
                continue
            if SPLIT_RE.match(line):
                part = "actual" if part == "expected" else "expected"
                continue
            if part:
                cur[part].append(line.rstrip("\n"))
                continue
            hm = HEADER_RE.match(line)
            if hm and not cur["kind"]:
                cur["kind"] = hm.group(1)
                cur["detail"] = hm.group(2).strip()
                continue
            if not cur["kind"] and line.strip():
                cur["detail"] = (cur["detail"] + " " + line.strip()).strip()[:300]
    return records


def markers(lines):
    c = Counter()
    for line in lines:
        for marker in MARKER_RE.findall(line):
            for name in marker.split(","):
                name = name.strip()
                if name:
                    c[name] += 1
    return c


def fixture_of(rec):
    d = rec["detail"]
    m = re.search(r"differs from file content:\s*(\S+)", d)
    base = m.group(1) if m else "?"
    return f"{rec['test']} [{base}]"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("log")
    ap.add_argument("--top", type=int, default=40)
    ap.add_argument("--diag")
    ap.add_argument("--cluster", action="store_true")
    ap.add_argument("--group")
    args = ap.parse_args()

    records = parse(args.log)
    print(f"failed testcases: {len(records)}")

    kinds = Counter(r["kind"] or "?" for r in records)
    print("\n== 失败类型 ==")
    for k, v in kinds.most_common():
        print(f"    {v:5d}  {k}")

    non_diff = [r for r in records if not r["expected"] and not r["actual"]]
    if non_diff:
        print(f"\n== 非 diff 失败 ({len(non_diff)}) ==")
        reasons = Counter(r["detail"][:120] for r in non_diff)
        for k, v in reasons.most_common(args.top):
            print(f"    {v:5d}  {k}")

    missing_c = Counter()
    extra_c = Counter()
    sig_files = defaultdict(set)
    for r in records:
        if not r["expected"] and not r["actual"]:
            continue
        exp = markers(r["expected"])
        act = markers(r["actual"])
        miss = exp - act
        extra = act - exp
        for n, v in miss.items():
            missing_c[n] += v
        for n, v in extra.items():
            extra_c[n] += v
        sig = (
            ",".join(sorted(miss)) or "-",
            ",".join(sorted(extra)) or "-",
        )
        sig_files[sig].add(fixture_of(r))
        r["miss"], r["extra"] = miss, extra

    print("\n== MISSING (期望有、实际无) ==")
    for k, v in missing_c.most_common(args.top):
        print(f"    {v:5d}  {k}")
    print("\n== EXTRA (实际有、期望无) ==")
    for k, v in extra_c.most_common(args.top):
        print(f"    {v:5d}  {k}")

    if args.cluster:
        print("\n== 按 (missing|extra) 诊断名签名聚类 ==")
        for sig, files in sorted(sig_files.items(), key=lambda kv: -len(kv[1]))[: args.top]:
            print(f"\n[{len(files)}] missing={sig[0]} | extra={sig[1]}")
            for f in sorted(files)[:10]:
                print(f"      {f}")

    if args.diag:
        print(f"\n== 涉及 {args.diag} 的 fixtures ==")
        seen = set()
        for r in records:
            if not r.get("miss") and not r.get("extra"):
                continue
            tag = []
            if args.diag in r.get("miss", {}):
                tag.append("MISSING")
            if args.diag in r.get("extra", {}):
                tag.append("EXTRA")
            if not tag:
                continue
            key = fixture_of(r)
            if key in seen:
                continue
            seen.add(key)
            print(f"  {'/'.join(tag):14s} {key}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
