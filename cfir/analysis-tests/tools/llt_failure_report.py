#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""LLT 失败聚合报告。

从 `cfir/analysis-tests/build/test-results/test/*.xml` 读取失败用例，
解析断言消息中的「预期 / 得到」两段渲染文本，按诊断标记（`<!NAME!>`）
计算缺失（missing，EXP 有 ACT 无）与多余（extra，ACT 有 EXP 无）的差集，
再按诊断名聚合，用于定位问题族的共享责任方。

用法：
    python cfir/analysis-tests/tools/llt_failure_report.py [--top N] [--diag NAME] [--suite SUBSTR]
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

RESULT_GLOB = "cfir/analysis-tests/build/test-results/test/*.xml"

# 断言消息中的分隔标题（渲染为中文），用宽松模式匹配以避开编码差异
SPLIT_RE = re.compile(r"={3,}[^\n=]*={3,}")
MARKER_RE = re.compile(r"<!([A-Za-z_0-9]+(?:!!)?[A-Za-z_0-9]*)!>")


def load_failures(result_glob: str = RESULT_GLOB):
    """产出 (suite, test, message) 三元组。"""
    for path in sorted(glob.glob(result_glob)):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        suite = root.get("name") or os.path.basename(path)
        for case in root.iter("testcase"):
            for node in list(case.iter("failure")) + list(case.iter("error")):
                msg = node.get("message") or (node.text or "")
                yield suite, case.get("name") or "?", msg


def split_expected_actual(message: str):
    """把断言消息切成（预期, 得到）两段；无法切分时返回 (None, None)。"""
    parts = SPLIT_RE.split(message)
    # 形如 [前言, 预期正文, 得到正文]
    if len(parts) < 3:
        return None, None
    return parts[1], parts[2]


def marker_counter(text: str) -> Counter:
    return Counter(MARKER_RE.findall(text or ""))


def diff_markers(expected: str, actual: str):
    """返回 (missing, extra) 两个 Counter。"""
    exp = marker_counter(expected)
    act = marker_counter(actual)
    missing = exp - act
    extra = act - exp
    return missing, extra


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--top", type=int, default=40)
    ap.add_argument("--diag", default=None, help="只列出该诊断名相关的用例")
    ap.add_argument("--suite", default=None, help="套件名子串过滤")
    ap.add_argument("--exclude", default="Macro", help="排除的套件名子串（默认 Macro）")
    ap.add_argument("--mode", default="summary", choices=["summary", "cases", "rangeonly"])
    args = ap.parse_args()

    missing_total = Counter()
    extra_total = Counter()
    diag_cases = defaultdict(set)
    unparsed = 0
    total = 0
    range_only_cases = []

    for suite, test, message in load_failures():
        if args.exclude and args.exclude in suite:
            continue
        if args.suite and args.suite not in suite:
            continue
        total += 1
        expected, actual = split_expected_actual(message)
        if expected is None:
            unparsed += 1
            continue
        missing, extra = diff_markers(expected, actual)
        if not missing and not extra:
            # 标记集合一致 -> 只可能是范围/文本差异
            range_only_cases.append((suite, test))
        for name, count in missing.items():
            missing_total[name] += count
            diag_cases[name].add((suite, test))
        for name, count in extra.items():
            extra_total[name] += count
            diag_cases[name].add((suite, test))

    if args.mode == "rangeonly":
        print(f"标记集合一致（疑似范围/文本差异）用例数：{len(range_only_cases)}")
        for suite, test in range_only_cases[: args.top]:
            print(f"  {suite}  ::  {test}")
        return 0

    if args.diag:
        cases = sorted(diag_cases.get(args.diag, ()))
        print(f"诊断 {args.diag}：missing={missing_total[args.diag]} extra={extra_total[args.diag]} 用例数={len(cases)}")
        for suite, test in cases[: args.top]:
            print(f"  {suite}  ::  {test}")
        return 0

    print(f"失败用例总数={total}  无法解析断言={unparsed}  标记一致（范围/文本差）={len(range_only_cases)}")
    print()
    print(f"{'诊断名':<58}{'missing':>9}{'extra':>8}{'用例数':>8}")
    keys = set(missing_total) | set(extra_total)
    ranked = sorted(keys, key=lambda k: -(missing_total[k] + extra_total[k]))
    for name in ranked[: args.top]:
        print(f"{name:<58}{missing_total[name]:>9}{extra_total[name]:>8}{len(diag_cases[name]):>8}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
