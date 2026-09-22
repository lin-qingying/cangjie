#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""临时探针：单构造 cjc 诊断（纯净源码，Windows 路径），支持多 SDK。"""

from __future__ import annotations

import io
import json
import os
import subprocess
import sys
import tempfile

SDKS = {
    "1.0.5": (r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe", r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5"),
    "1.0.0": (r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.0\bin\cjc.exe", r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.0"),
}


def probe(src: str, sdk: str = "1.0.5", raw: bool = False, extra: list[str] | None = None):
    exe, home = SDKS[sdk]
    wd = tempfile.mkdtemp(prefix="cjc-one-")
    path = os.path.join(wd, "probe.cj")
    io.open(path, "w", encoding="utf-8", newline="").write(src)
    env = dict(os.environ)
    env["CANGJIE_HOME"] = home
    cmd = [exe] + ([] if raw else ["--diagnostic-format=json"]) + (extra or []) + [path, "-o", os.path.join(wd, "a.out")]
    p = subprocess.run(cmd, capture_output=True, text=True, errors="replace", cwd=wd, env=env)
    out = (p.stdout or "") + (p.stderr or "")
    if raw:
        return p.returncode, out
    try:
        d = json.loads(out)
    except Exception:  # noqa: BLE001
        return p.returncode, out
    rows = []
    for g in d.get("Diags", []):
        r = (g.get("MainHint") or {}).get("Range") or {}
        rows.append(
            "L%s[%s,%s) %s :: %s"
            % (
                (g.get("Location") or {}).get("Line"),
                (r.get("Begin") or {}).get("Column"),
                (r.get("End") or {}).get("Column"),
                g["DiagKind"],
                g.get("Message", ""),
            )
        )
    return p.returncode, "\n".join(rows) + f"\n  Num={d.get('Num')}"


if __name__ == "__main__":
    # 用法: python cjc_one_probe.py <file.cj> [sdk] [--raw]
    src = io.open(sys.argv[1], encoding="utf-8").read()
    sdk = sys.argv[2] if len(sys.argv) > 2 and not sys.argv[2].startswith("--") else "1.0.5"
    raw = "--raw" in sys.argv
    code, out = probe(src, sdk, raw=raw)
    print(f"### sdk={sdk} exit={code}")
    print(out.strip())
