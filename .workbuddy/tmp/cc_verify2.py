import os, re, subprocess, json

CJC = r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe"
SRC = r"D:\code\intellij\cangjie\cfir\analysis-tests\testData\llt\constraint_check"
OUT = r"D:\code\intellij\cangjie\tmp\cc-verify"

FILES = {
    "expose4": "expose4.cj",
    "expose5": "expose5.cj",
    "test12": "constraint_check_test12.cj",
    "solve1": "solve1.cj",
    "instantiate7": "constraint_instantiate_test7.cj",
    "option01": "option_with_element_01.cj",
}

marker = re.compile(r"<!>|<!([A-Z0-9_]+)!>")
SUMMARY = []
for tag, name in FILES.items():
    probe = os.path.join(OUT, f"{tag}.cj")
    with open(os.path.join(SRC, name), encoding="utf-8") as f:
        stripped = marker.sub("", f.read())
    stripped = re.sub(r'^\s*//\s*(RUN_PIPELINE_TILL|ENABLE_INTEROP_CJMAPPING|TARGET_INTEROP_LANGUAGE)[^\n]*\n',
                      '', stripped, flags=re.M)
    with open(probe, "w", encoding="utf-8") as f:
        f.write(stripped)
    cp = subprocess.run([CJC, probe, "--diagnostic-format=json", "--output-type=staticlib",
                         "-o", os.path.join(OUT, f"{tag}.out")],
                        capture_output=True, cwd=OUT, text=True, encoding="utf-8", errors="replace")
    raw = (cp.stdout or "") + (cp.stderr or "")
    data = None
    try:
        data = json.loads(raw[raw.index("{"):])
    except Exception as e:
        print(f"### {tag}: json parse fail ({e})")
    print("=" * 92)
    print(f"### {tag}   ({name})   cjc exit={cp.returncode}")
    if data:
        for d in data.get("Diags", []):
            loc = d.get("Location") or {}
            sev = d.get("Severity", "?")
            kind = d.get("DiagKind", "?")
            msg = (d.get("Message") or "").replace("\n", " ")[:110]
            mark = "  " if sev != "error" else "E "
            print(f" {mark}L{loc.get('Line')}:C{loc.get('Column')}  {kind}  -- {msg}")
            SUMMARY.append((tag, sev, kind))
print()
print("=" * 92)
print("汇总（只有 error 级）")
for tag in FILES:
    ks = sorted({k for t, s, k in SUMMARY if t == tag and s == "error"})
    print(f"  {tag:14s} errors={len([1 for t,s,k in SUMMARY if t==tag and s=='error'])}  {ks}")
