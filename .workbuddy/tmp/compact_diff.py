#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Compact marker-level diff for LLT failures read from test-results XML.

Usage:
    python compact_diff.py <suite-substring> [<test-substring>] [--limit N]
    python compact_diff.py <suite-substring> --sig      # one-line signature per case
"""
from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

RESULTS = "cfir/analysis-tests/build/test-results/test/*.xml"
SPLIT_RE = re.compile(r"^\s*={3,}[^\n=]*={3,}\s*$", re.M)
MARKER_RE = re.compile(r"<!([^!<>]+)!>")


def split_exp_act(message: str):
    parts = SPLIT_RE.split(message)
    if len(parts) < 3:
        return None, None
    return parts[1], parts[2]


def iter_failures():
    for path in sorted(glob.glob(RESULTS)):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        suite = root.get("name") or os.path.basename(path)
        for case in root.iter("testcase"):
            for node in list(case.iter("failure")) + list(case.iter("error")):
                yield suite, case.get("name") or "?", node.get("message") or ""


def norm(lines):
    return [ln.strip() for ln in lines]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("suite")
    ap.add_argument("test", nargs="?", default=None)
    ap.add_argument("--limit", type=int, default=30)
    ap.add_argument("--sig", action="store_true")
    ap.add_argument("--no-macro", action="store_true")
    args = ap.parse_args()

    shown = 0
    for suite, test, msg in iter_failures():
        if args.suite not in suite:
            continue
        if args.no_macro and "Macro" in suite:
            continue
        if args.test and args.test not in test:
            continue
        exp, act = split_exp_act(msg)
        if exp is None:
            continue
        shown += 1
        if shown > args.limit:
            break
        e, a = norm(exp.splitlines()), norm(act.splitlines())
        if args.sig:
            em, am = sorted(MARKER_RE.findall(exp)), sorted(MARKER_RE.findall(act))
            miss = [x for x in em if x not in am]
            extra = [x for x in am if x not in em]
            tag = "RANGE-ONLY" if not miss and not extra else ""
            fam = suite.split("cfir.analysis.tests.")[-1]
            print(f"{fam}\n    {test:42s} miss={sorted(set(miss))} extra={sorted(set(extra))} {tag}")
            continue
        print("=" * 90)
        print(f"{suite.split('tests.')[-1]} :: {test}")
        # aligned diff over lines present in either
        import difflib

        for line in difflib.unified_diff(e, a, lineterm="", n=1):
            if line.startswith(("---", "+++")):
                continue
            print(line)
    print(f"\n[shown={shown}]")
    return 0


if __name__ == "__main__":
    sys.exit(main())
