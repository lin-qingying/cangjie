#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按官方 cjc 报告的精确跨度，为 fixture 补写内联诊断标记。

标记范围取自 cjc 输出的 `==> file:line:col` 与其下方 `^^^^` 插入符宽度，
而不是从本仓库 CFIR 的实际输出复制——这样测试是否通过本身就成为
「CFIR 范围是否与官方一致」的证明。

用法：
    python cfir/analysis-tests/tools/cjc_apply_markers.py --diag UNREACHABLE_PATTERN \
        --official "unreachable pattern" --files <fixture 相对路径>...
"""

from __future__ import annotations

import argparse
import io
import os
import re
import shutil
import subprocess
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cjc_crosscheck import REPO, CJC, strip_markers, FILE_SECTION_RE  # noqa: E402

# 形如： ==> name.cj:5:10:  然后若干行后出现   |      ^^^^^^
LOC_RE = re.compile(r"==>\s*(\S+?):(\d+):(\d+):")


def collect_spans(raw: str, official: str):
    """从 cjc 原始输出中收集 (file, line, col, width) 跨度，仅取诊断文本匹配 official 的那些。"""
    spans = []
    lines = raw.splitlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        # 诊断头：warning: xxx / error: xxx （可能带 ANSI 残留）
        head = re.match(r"^(warning|error)\S*:\s*(.+?)\s*$", line)
        if head and official in head.group(2):
            # 向下找位置行与插入符行
            loc = None
            width = None
            j = i + 1
            while j < min(i + 12, len(lines)):
                if loc is None:
                    m = LOC_RE.search(lines[j])
                    if m:
                        loc = (m.group(1), int(m.group(2)), int(m.group(3)))
                else:
                    carets = re.search(r"\^+", lines[j])
                    if carets:
                        width = len(carets.group(0))
                        break
                j += 1
            if loc and width:
                spans.append((loc[0], loc[1], loc[2], width))
            i = j
        i += 1
    return spans


def run_cjc_raw(fixture_rel: str):
    abspath = os.path.join(REPO, fixture_rel)
    src = io.open(abspath, encoding="utf-8").read()
    clean = strip_markers(src)
    wd = tempfile.mkdtemp(prefix="cjc-mark-")
    written = []
    sections = list(FILE_SECTION_RE.finditer(clean))
    try:
        if sections:
            for idx, sec in enumerate(sections):
                name = os.path.basename(sec.group(1))
                start = sec.end()
                end = sections[idx + 1].start() if idx + 1 < len(sections) else len(clean)
                dst = os.path.join(wd, name)
                io.open(dst, "w", encoding="utf-8").write(clean[start:end])
                written.append(dst)
        else:
            dst = os.path.join(wd, os.path.basename(fixture_rel))
            io.open(dst, "w", encoding="utf-8").write(clean)
            written.append(dst)
        proc = subprocess.run(
            [CJC, "--no-sub-pkg"] + written + ["-o", os.path.join(wd, "a.out")],
            capture_output=True, text=True, errors="replace", cwd=wd,
        )
        return (proc.stdout or "") + (proc.stderr or ""), bool(sections)
    finally:
        shutil.rmtree(wd, ignore_errors=True)


def apply_markers(fixture_rel: str, diag: str, official: str, dry_run: bool = False):
    abspath = os.path.join(REPO, fixture_rel)
    original = io.open(abspath, encoding="utf-8").read()
    if MARKER_PRESENT.search(original):
        existing = MARKER_PRESENT.findall(original)
        if diag in existing:
            return f"跳过（已含 {diag} 标记）", None

    raw, multifile = run_cjc_raw(fixture_rel)
    if multifile:
        return "跳过（多文件 fixture，行号需人工核对）", None
    spans = collect_spans(raw, official)
    if not spans:
        return "跳过（cjc 未报此诊断）", None

    # 剥离标记后的文本才是 cjc 看到的坐标系；这些 fixture 本就无标记，直接用原文
    lines = original.splitlines(keepends=True)
    # 按行倒序插入，避免前面的插入影响后面的列号
    for _f, ln, col, width in sorted(spans, key=lambda s: (-s[1], -s[2])):
        if ln - 1 >= len(lines):
            return f"失败（行号 {ln} 越界）", None
        text = lines[ln - 1]
        start = col - 1
        end = start + width
        lines[ln - 1] = text[:start] + f"<!{diag}!>" + text[start:end] + "<!>" + text[end:]
    updated = "".join(lines)
    if not dry_run:
        io.open(abspath, "w", encoding="utf-8", newline="").write(updated)
    return f"已补 {len(spans)} 处标记", updated


MARKER_PRESENT = re.compile(r"<!([A-Za-z_0-9]+)!>")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--diag", required=True)
    ap.add_argument("--official", required=True)
    ap.add_argument("--files", nargs="+", required=True)
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    for rel in args.files:
        status, _ = apply_markers(rel, args.diag, args.official, args.dry_run)
        print(f"{status:<28} {rel}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
