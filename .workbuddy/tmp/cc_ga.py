import os, re, subprocess, json

CJC = r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe"
OUT = r"D:\code\intellij\cangjie\tmp\cc-verify"
TD = r"D:\code\intellij\cangjie\cfir\analysis-tests\testData"

CASES = {
    "ga_rich": os.path.join(TD, "diagnostics", "generic-access", "upperBoundsMemberAndMethodRich.cj"),
    "ga_d2": os.path.join(TD, "diagnostics2", "generic-access", "upperBoundsMemberAndMethod.cj"),
}

marker = re.compile(r"<!>|<!([A-Z0-9_]+)!>")
for tag, path in CASES.items():
    with open(path, encoding="utf-8") as f:
        text = f.read()
    stripped = marker.sub("", text)
    stripped = re.sub(r'^\s*//\s*RUN_PIPELINE_TILL[^\n]*\n', '', stripped, flags=re.M)
    probe = os.path.join(OUT, f"{tag}.cj")
    with open(probe, "w", encoding="utf-8") as f:
        f.write(stripped)
    cp = subprocess.run([CJC, probe, "--diagnostic-format=json", "--output-type=staticlib",
                         "-o", os.path.join(OUT, f"{tag}.out")],
                        capture_output=True, cwd=OUT, text=True, encoding="utf-8", errors="replace")
    raw = (cp.stdout or "") + (cp.stderr or "")
    print("=" * 92)
    print(f"### {tag}  exit={cp.returncode}")
    try:
        data = json.loads(raw[raw.index("{"):])
    except Exception as e:
        print("  parse fail", e, raw[:400]); continue
    for d in data.get("Diags", []):
        loc = d.get("Location") or {}
        print(f"  [{d.get('Severity')}] L{loc.get('Line')}:C{loc.get('Column')} {d.get('DiagKind')} -- {(d.get('Message') or '')[:90]}")
