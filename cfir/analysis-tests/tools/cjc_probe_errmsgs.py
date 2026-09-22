#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""临时探针：对指定的 ErrMsgs fixture 跑官方 cjc（1.0.5 / 1.0.0），打印诊断。

用法:
    python cfir/analysis-tests/tools/cjc_probe_errmsgs.py sync_0 var_decl_0
"""

from __future__ import annotations

import io
import os
import subprocess
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cjc_crosscheck import REPO, strip_markers, FILE_SECTION_RE  # noqa: E402
import re  # noqa: E402

SDKS = {
    "1.0.5": r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe",
    "1.0.0": r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.0\bin\cjc.exe",
}
SDK_ROOTS = {
    "1.0.5": r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5",
    "1.0.0": r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.0",
}
REL = "cfir/analysis-tests/testData/llt/ErrMsgs"


def run(sdk: str, fixture_rel: str) -> tuple[int, str]:
    abspath = os.path.join(REPO, fixture_rel)
    src = io.open(abspath, encoding="utf-8").read()
    clean = strip_markers(src).replace("\r\n", "\n")
    wd = tempfile.mkdtemp(prefix="cjc-errmsgs-")
    files = []
    sections = list(FILE_SECTION_RE.finditer(clean))
    if sections:
        for idx, sec in enumerate(sections):
            name = os.path.basename(sec.group(1))
            start = sec.end()
            end = sections[idx + 1].start() if idx + 1 < len(sections) else len(clean)
            dst = os.path.join(wd, name)
            io.open(dst, "w", encoding="utf-8", newline="").write(clean[start:end])
            files.append(dst)
    else:
        dst = os.path.join(wd, os.path.basename(fixture_rel))
        io.open(dst, "w", encoding="utf-8", newline="").write(clean)
        files.append(dst)

    env = dict(os.environ)
    env["CANGJIE_HOME"] = SDK_ROOTS[sdk]
    cmd = [SDKS[sdk], "--no-sub-pkg", "--diagnostic-format=json"] + files + ["-o", os.path.join(wd, "a.out")]
    proc = subprocess.run(cmd, capture_output=True, text=True, errors="replace", cwd=wd, env=env)
    return proc.returncode, (proc.stdout or "") + (proc.stderr or "")


def compact(out: str) -> str:
    """从 JSON 输出提取 (DiagKind, line, colBegin, colEnd, message) 紧凑表。"""
    import json

    try:
        data = json.loads(out)
    except Exception:  # noqa: BLE001
        return out.strip()[:2000]
    lines = []
    for d in data.get("Diags", []):
        rng = (d.get("MainHint") or {}).get("Range") or {}
        b = (rng.get("Begin") or {}).get("Column")
        e = (rng.get("End") or {}).get("Column")
        ln = (d.get("Location") or {}).get("Line")
        lines.append(f"  L{ln} [{b},{e}) {d['DiagKind']}  :: {d.get('Message','')}")
    n = data.get("Num", {})
    lines.append(f"  -- Errors={n.get('Errors')} Warnings={n.get('Warnings')}")
    return "\n".join(lines)


def main() -> int:
    names = sys.argv[1:]
    for name in names:
        fixture_rel = f"{REL}/{name}.cj"
        print("=" * 78)
        print(f"### {name}.cj")
        for sdk in SDKS:
            code, out = run(sdk, fixture_rel)
            print(f"  --- sdk={sdk} exit={code}")
            print(compact(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
