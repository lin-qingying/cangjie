#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""用官方 cjc 交叉校验 LLT 失败，判定「fixture 错」还是「CFIR 错」。

流程：
1. 解析 `tests-gen/**/*TestGenerated.kt`，建立 (套件类, 测试方法) -> fixture 路径 的映射；
2. 对指定诊断族的失败用例，取出 fixture，剥离 `<!DIAG!>...<!>` 内联标记后交给官方 `cjc` 编译；
3. 把 cjc 的诊断行号/列号与 fixture 期望标记、CFIR 实际标记三方对齐；
4. 输出判定：cjc 与 CFIR 一致 => fixture 期望缺失；cjc 与 fixture 一致 => CFIR 误报。

用法：
    python cfir/analysis-tests/tools/cjc_crosscheck.py --diag UNREACHABLE_PATTERN [--limit 20]
"""

from __future__ import annotations

import argparse
import glob
import io
import os
import re
import shutil
import subprocess
import sys
import tempfile
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from llt_failure_report import load_failures, split_expected_actual, diff_markers, MARKER_RE  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
CJC = r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe"
TESTS_GEN = os.path.join(REPO, "cfir", "analysis-tests", "tests-gen")

FILE_SECTION_RE = re.compile(r"^//\s*FILE:\s*(\S+)\s*$", re.MULTILINE)
CJC_DIAG_RE = re.compile(r"^(warning|error)\S*:\s*(.+?)\s*$", re.MULTILINE)
CJC_LOC_RE = re.compile(r"^\s*==>\s*(\S+?):(\d+):(\d+):", re.MULTILINE)


def build_test_map() -> dict:
    """返回 {(简短套件名, 方法名): fixture 相对路径}。"""
    mapping = {}
    for path in glob.glob(os.path.join(TESTS_GEN, "**", "*TestGenerated.kt"), recursive=True):
        text = io.open(path, encoding="utf-8").read()
        cls = os.path.basename(path)[: -len(".kt")]
        # 逐个 fun testXxx() { runTest("...") } 抓取
        for m in re.finditer(r"fun\s+(test\w+)\(\)\s*\{\s*runTest\(\"([^\"]+)\"\)", text):
            mapping[(cls, m.group(1) + "()")] = m.group(2)
    return mapping


def strip_markers(src: str) -> str:
    src = MARKER_RE.sub("", src)
    return src.replace("<!>", "")


def expected_marker_lines(src: str):
    """返回 fixture 中每个内联标记所在行号（1 起）与诊断名。"""
    out = []
    line = 1
    i = 0
    while i < len(src):
        m = MARKER_RE.match(src, i)
        if m:
            out.append((line, m.group(1)))
            i = m.end()
            continue
        if src.startswith("<!>", i):
            i += 3
            continue
        if src[i] == "\n":
            line += 1
        i += 1
    return out


def run_cjc(fixture_rel: str, workdir: str):
    """编译 fixture（支持 `// FILE:` 多文件），返回 (exit, 诊断列表)。"""
    abspath = os.path.join(REPO, fixture_rel)
    src = io.open(abspath, encoding="utf-8").read()
    clean = strip_markers(src)

    sections = list(FILE_SECTION_RE.finditer(clean))
    written = []
    if sections:
        for idx, sec in enumerate(sections):
            name = os.path.basename(sec.group(1))
            start = sec.end()
            end = sections[idx + 1].start() if idx + 1 < len(sections) else len(clean)
            body = clean[start:end]
            # 保持行号：把被删掉的 FILE 头之前的内容替换为等量空行不可行，
            # 多文件 fixture 的行号本就按段落各自计，这里按段落原样写出。
            dst = os.path.join(workdir, name)
            io.open(dst, "w", encoding="utf-8").write(body)
            written.append(dst)
    else:
        dst = os.path.join(workdir, os.path.basename(fixture_rel))
        io.open(dst, "w", encoding="utf-8").write(clean)
        written.append(dst)

    cmd = [CJC, "--no-sub-pkg"] + written + ["-o", os.path.join(workdir, "a.out")]
    proc = subprocess.run(cmd, capture_output=True, text=True, errors="replace", cwd=workdir)
    out = (proc.stdout or "") + (proc.stderr or "")

    diags = []
    # 诊断与位置在输出中成对出现，按出现顺序配对
    kinds = [(m.start(), m.group(1), m.group(2)) for m in CJC_DIAG_RE.finditer(out)]
    locs = [(m.start(), m.group(1), int(m.group(2)), int(m.group(3))) for m in CJC_LOC_RE.finditer(out)]
    for pos, kind, text in kinds:
        loc = next((l for l in locs if l[0] > pos), None)
        if loc:
            diags.append({"kind": kind, "text": text, "file": loc[1], "line": loc[2], "col": loc[3]})
        else:
            diags.append({"kind": kind, "text": text, "file": None, "line": None, "col": None})
    return proc.returncode, diags, out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--diag", required=True, help="诊断名，例如 UNREACHABLE_PATTERN")
    ap.add_argument("--official", default=None, help="对应官方诊断文本子串，例如 'unreachable pattern'")
    ap.add_argument("--limit", type=int, default=25)
    ap.add_argument("--psi", action="store_true", help="用 PSI 套件（默认非 PSI）")
    args = ap.parse_args()

    tmap = build_test_map()
    seen = set()
    rows = []
    for suite, test, msg in load_failures():
        if "Macro" in suite:
            continue
        is_psi = "PsiTest" in suite
        if is_psi != args.psi:
            continue
        exp, act = split_expected_actual(msg)
        if exp is None:
            continue
        m, e = diff_markers(exp, act)
        if set(m) | set(e) != {args.diag}:
            continue
        cls = suite.split(".")[-1].split("$")[0]
        key = (cls, test)
        fixture = tmap.get(key)
        if not fixture or fixture in seen:
            continue
        seen.add(fixture)
        rows.append((suite, test, fixture, sum(m.values()), sum(e.values())))

    print(f"待校验 fixture 数：{len(rows)}（诊断={args.diag}, psi={args.psi}）\n")
    workroot = tempfile.mkdtemp(prefix="cjc-xcheck-")
    verdicts = defaultdict(list)
    try:
        for suite, test, fixture, miss, extra in rows[: args.limit]:
            wd = tempfile.mkdtemp(dir=workroot)
            try:
                code, diags, raw = run_cjc(fixture, wd)
            except Exception as ex:  # noqa: BLE001
                print(f"[跳过] {fixture}: {ex}")
                continue
            if args.official:
                official_hits = [d for d in diags if args.official in d["text"]]
            else:
                official_hits = diags
            cfir_count = extra  # CFIR 比 fixture 多报的数量
            fixture_expected = len(
                [n for _, n in expected_marker_lines(io.open(os.path.join(REPO, fixture), encoding="utf-8").read()) if n == args.diag]
            )
            cfir_total = fixture_expected + extra - miss
            n_off = len(official_hits)
            if n_off == cfir_total and n_off != fixture_expected:
                verdict = "FIXTURE_WRONG(缺期望)"
            elif n_off == fixture_expected and n_off != cfir_total:
                verdict = "CFIR_WRONG"
            elif n_off == fixture_expected == cfir_total:
                verdict = "COUNT_TIE(范围差异)"
            else:
                verdict = "THREE_WAY_DIFF"
            verdicts[verdict].append(fixture)
            print(f"{verdict:<22} cjc={n_off:<3} fixture={fixture_expected:<3} cfir={cfir_total:<3} exit={code}  {fixture}")
    finally:
        shutil.rmtree(workroot, ignore_errors=True)

    print("\n== 判定汇总 ==")
    for v, files in sorted(verdicts.items(), key=lambda kv: -len(kv[1])):
        print(f"{v:<22} {len(files)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
