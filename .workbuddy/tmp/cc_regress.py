import os, re, subprocess, json

CJC = r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe"
OUT = r"D:\code\intellij\cangjie\tmp\cc-verify"
TD = r"D:\code\intellij\cangjie\cfir\analysis-tests\testData\llt"

CASES = {
    "r_invalid5": os.path.join(TD, "class", "class_no_override_modifier_invalid_5.cj"),
    "r_invalid6": os.path.join(TD, "class", "class_no_override_modifier_invalid_6.cj"),
    "r_invalid7": os.path.join(TD, "class", "class_no_override_modifier_invalid_7.cj"),
    "r_invalid8": os.path.join(TD, "class", "class_no_override_modifier_invalid_8.cj"),
    "r_invalid9": os.path.join(TD, "class", "class_no_override_modifier_invalid_9.cj"),
    "r_iface_impl4": os.path.join(TD, "class", "interface_implement4.cj"),
    "r_iface_impl_inv4": os.path.join(TD, "class", "interface_implement_invalid4.cj"),
    "r_prop4": os.path.join(TD, "interface", "interface_property4.cj"),
    "r_prop5": os.path.join(TD, "interface", "interface_property5.cj"),
}

marker = re.compile(r"<!>|<!([A-Z0-9_]+)!>")
for tag, path in CASES.items():
    if not os.path.exists(path):
        print(f"{tag:20s} (fixture 不存在: {path})")
        continue
    with open(path, encoding="utf-8") as f:
        stripped = marker.sub("", f.read())
    stripped = re.sub(r'^\s*//\s*(RUN_PIPELINE_TILL|ENABLE_INTEROP_CJMAPPING|TARGET_INTEROP_LANGUAGE)[^\n]*\n', '', stripped, flags=re.M)
    probe = os.path.join(OUT, f"{tag}.cj")
    with open(probe, "w", encoding="utf-8") as f:
        f.write(stripped)
    cp = subprocess.run([CJC, probe, "--diagnostic-format=json", "--output-type=staticlib",
                         "-o", os.path.join(OUT, f"{tag}.out")],
                        capture_output=True, cwd=OUT, text=True, encoding="utf-8", errors="replace")
    raw = (cp.stdout or "") + (cp.stderr or "")
    try:
        data = json.loads(raw[raw.index("{"):])
        errs = [d for d in data.get("Diags", []) if d.get("Severity") == "error"]
    except Exception:
        errs = [{"DiagKind": "PARSE_FAIL", "Message": raw[:100]}]
    kinds = ", ".join(sorted({d.get("DiagKind", "?") for d in errs})) or "(no error)"
    print(f"{tag:20s} exit={cp.returncode}  {kinds}")
