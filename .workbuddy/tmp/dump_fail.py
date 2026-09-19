#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Dump LLT failure diffs from build/test-results/test/*.xml.

Usage:
    python dump_fail.py <suite-substring> [<test-substring>] [--limit N] [--raw]
"""
from __future__ import annotations

import argparse
import glob
import os
import sys
import xml.etree.ElementTree as ET

RESULTS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "cfir", "analysis-tests", "build", "test-results", "test", "*.xml",
)
RESULTS = "cfir/analysis-tests/build/test-results/test/*.xml"


def iter_failures():
    for path in sorted(glob.glob(RESULTS)):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        suite = root.get("name") or os.path.basename(path)
        for case in root.iter("testcase"):
            for node in list(case.iter("failure")) + list(case.iter("error")):
                yield suite, case.get("name") or "?", (node.get("message") or ""), (node.text or "")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("suite")
    ap.add_argument("test", nargs="?", default=None)
    ap.add_argument("--limit", type=int, default=20)
    ap.add_argument("--raw", action="store_true")
    args = ap.parse_args()

    shown = 0
    for suite, test, msg, text in iter_failures():
        if args.suite and args.suite not in suite:
            continue
        if args.test and args.test not in test:
            continue
        shown += 1
        if shown > args.limit:
            break
        print("=" * 100)
        print(f"{suite}\n  :: {test}")
        print("-" * 100)
        body = msg if args.raw else msg
        print(body)
        if args.raw:
            print(text)
    print(f"\n[matched shown={shown}]")
    return 0


if __name__ == "__main__":
    sys.exit(main())
