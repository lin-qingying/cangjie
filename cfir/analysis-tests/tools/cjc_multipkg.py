#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""用官方 cjc 编译多包 LLT fixture（`// FILE:` + `package` 分包）。

`cjc_crosscheck.py` 把所有 `// FILE:` 段落平铺给一次 cjc 调用，对单包 fixture 有效，
但多包 fixture 会直接失败在 `found more than one package declaration for the package`。
本驱动按 `package` 声明分目录，先把依赖包编成 staticlib（同时产出 `.cjo`），
再用 `--import-path` 编译根包，从而拿到真正的语义诊断。

用法：
    python cfir/analysis-tests/tools/cjc_multipkg.py <fixture 相对路径> [...]
"""

from __future__ import annotations

import io
import os
import re
import subprocess
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cjc_crosscheck import CJC, FILE_SECTION_RE, REPO, strip_markers  # noqa: E402

PACKAGE_RE = re.compile(r"^\s*(?:macro\s+)?package\s+([A-Za-z_][\w.]*)", re.MULTILINE)
IMPORT_RE = re.compile(r"^\s*(?:public|internal|protected|private)?\s*import\s+([A-Za-z_][\w.]*)", re.MULTILINE)
DIAG_RE = re.compile(r"^(warning|error)\S*:\s*(.+?)\s*$", re.MULTILINE)
LOC_RE = re.compile(r"^\s*==>\s*(\S+?):(\d+):(\d+):", re.MULTILINE)


def split_sections(fixture_rel: str):
    """返回 [(文件名, 正文)]；无 `// FILE:` 时视为单文件。"""
    src = strip_markers(io.open(os.path.join(REPO, fixture_rel), encoding="utf-8").read())
    sections = list(FILE_SECTION_RE.finditer(src))
    if not sections:
        return [(os.path.basename(fixture_rel), src)]
    out = []
    for idx, sec in enumerate(sections):
        start = sec.end()
        end = sections[idx + 1].start() if idx + 1 < len(sections) else len(src)
        out.append((os.path.basename(sec.group(1)), src[start:end]))
    return out


def group_by_package(sections):
    """返回 {包名: [(文件名, 正文)]}，根包用空串表示。"""
    groups = {}
    for name, body in sections:
        m = PACKAGE_RE.search(body)
        groups.setdefault(m.group(1) if m else "", []).append((name, body))
    return groups


def parse_diags(output: str):
    kinds = [(m.start(), m.group(1), m.group(2)) for m in DIAG_RE.finditer(output)]
    locs = [(m.start(), m.group(1), int(m.group(2)), int(m.group(3))) for m in LOC_RE.finditer(output)]
    diags = []
    for pos, kind, text in kinds:
        loc = next((l for l in locs if l[0] > pos), None)
        diags.append({
            "kind": kind, "text": text,
            "file": loc[1] if loc else None,
            "line": loc[2] if loc else None,
            "col": loc[3] if loc else None,
        })
    return diags


def compile_fixture(fixture_rel: str, workdir: str):
    """按依赖顺序编译各包，返回 (最终 exit, 诊断列表, 原始输出)。"""
    groups = group_by_package(split_sections(fixture_rel))
    deps = {pkg: set() for pkg in groups}
    for pkg, files in groups.items():
        for _, body in files:
            for imported in IMPORT_RE.findall(body):
                head = imported.split(".")[0]
                if head in groups and head != pkg:
                    deps[pkg].add(head)

    # 依赖包各自落到独立目录，根包最后编
    for pkg, files in groups.items():
        pkgdir = os.path.join(workdir, pkg or "__root__")
        os.makedirs(pkgdir, exist_ok=True)
        for name, body in files:
            io.open(os.path.join(pkgdir, name), "w", encoding="utf-8").write(body)

    outdir = os.path.join(workdir, "out")
    os.makedirs(outdir, exist_ok=True)

    ordered, pending = [], [p for p in groups if p]
    while pending:
        progressed = False
        for pkg in list(pending):
            if deps[pkg] <= set(ordered):
                ordered.append(pkg)
                pending.remove(pkg)
                progressed = True
        if not progressed:  # 依赖成环时退化为原顺序
            ordered.extend(pending)
            break

    combined, code = "", 0
    for pkg in ordered:
        cmd = [CJC, "-p", os.path.join(workdir, pkg), "--output-type=staticlib",
               "--import-path", outdir, "-o", os.path.join(outdir, f"lib{pkg}.a")]
        proc = subprocess.run(cmd, capture_output=True, text=True, errors="replace", cwd=outdir)
        combined += (proc.stdout or "") + (proc.stderr or "")
        code = code or proc.returncode

    if "" in groups:
        cmd = [CJC, "-p", os.path.join(workdir, "__root__"), "--import-path", outdir, "-L", outdir]
        for pkg in reversed(ordered):
            cmd.append(f"-l{pkg}")
        cmd += ["-o", os.path.join(workdir, "a.out")]
        proc = subprocess.run(cmd, capture_output=True, text=True, errors="replace", cwd=workdir)
        combined += (proc.stdout or "") + (proc.stderr or "")
        code = code or proc.returncode

    return code, parse_diags(combined), combined


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    root = tempfile.mkdtemp(prefix="cjc-multipkg-")
    for fixture in sys.argv[1:]:
        wd = tempfile.mkdtemp(dir=root)
        code, diags, raw = compile_fixture(fixture, wd)
        print(f"=== {fixture}  exit={code} 诊断数={len(diags)}")
        for d in diags:
            where = f"{d['file']}:{d['line']}:{d['col']}" if d["file"] else "?"
            print(f"    [{d['kind']}] {where}  {d['text']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
