import os, re, subprocess, json, sys, glob

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

os.makedirs(OUT, exist_ok=True)
marker = re.compile(r"<!>|<!([A-Z0-9_]+)!>")

for tag, name in FILES.items():
    src_path = os.path.join(SRC, name)
    with open(src_path, encoding="utf-8") as f:
        text = f.read()
    stripped = marker.sub("", text)
    stripped = re.sub(r'^\s*//\s*(RUN_PIPELINE_TILL|ENABLE_INTEROP_CJMAPPING|TARGET_INTEROP_LANGUAGE)[^\n]*\n',
                      '', stripped, flags=re.M)
    probe = os.path.join(OUT, f"{tag}.cj")
    with open(probe, "w", encoding="utf-8") as f:
        f.write(stripped)

    cp = subprocess.run(
        [CJC, probe, "--diagnostic-format=json", "--output-type=staticlib",
         "-o", os.path.join(OUT, f"{tag}.out")],
        capture_output=True, cwd=OUT, text=True, encoding="utf-8", errors="replace")
    print("=" * 90)
    print(f"### {tag}  ({name})  exit={cp.returncode}")
    raw = (cp.stdout or "") + (cp.stderr or "")
    diags = []
    for line in raw.splitlines():
        line = line.strip()
        if not line.startswith("{"):
            continue
        try:
            diags.append(json.loads(line))
        except Exception:
            pass
    if not diags:
        print("  (no json diagnostics)")
        print("  raw:", raw.strip()[:600])
    for d in diags:
        kind = d.get("diagKind") or d.get("kind") or d.get("name") or "?"
        sev = d.get("level") or d.get("severity") or ""
        rng = d.get("range") or {}
        beg = rng.get("begin") or {}
        msg = (d.get("message") or d.get("diagMessage") or "")[:120]
        print(f"  [{sev}] {kind} @ line{beg.get('line')} col{beg.get('column')} - {msg}")
    print("  errors:", sum(1 for d in diags if (d.get('level') or d.get('severity')) in ('error', 'Error')))
