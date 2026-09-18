import os, re, subprocess, json
CJC = r"C:/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc.exe"
OUT = r"D:/code/intellij/cangjie/tmp/cc-verify"
R = r"cfir/analysis-tests/testData"
PATHS = {
 "invalid5": f"{R}/llt/class/class_no_override_modifier_invalid_5.cj",
 "invalid6": f"{R}/llt/class/class_no_override_modifier_invalid_6.cj",
 "invalid7": f"{R}/llt/class/class_no_override_modifier_invalid_7.cj",
 "invalid8": f"{R}/llt/class/class_no_override_modifier_invalid_8.cj",
 "invalid9": f"{R}/llt/class/class_no_override_modifier_invalid_9/class_no_override_modifier_invalid_9.cj",
 "iface_impl4": f"{R}/llt/class/interface_implement_4.cj",
 "iface_impl_inv4": f"{R}/llt/class/interface_implement_invalid_4.cj",
 "prop4": f"{R}/llt/interface/interface_property/interface_property4.cj",
 "prop5": f"{R}/llt/interface/interface_property/interface_property5.cj",
}
marker = re.compile(r"<!>|<!([A-Z0-9_]+)!>")
for tag, rel in PATHS.items():
    p = os.path.join(r"D:/code/intellij/cangjie", rel)
    if not os.path.exists(p):
        print(f"{tag:16s} MISSING {rel}"); continue
    src = marker.sub("", open(p, encoding="utf-8").read())
    src = re.sub(r'^\s*//\s*(RUN_PIPELINE_TILL|ENABLE_INTEROP_CJMAPPING|TARGET_INTEROP_LANGUAGE)[^\n]*\n', '', src, flags=re.M)
    pr = os.path.join(OUT, f"x_{tag}.cj")
    open(pr, "w", encoding="utf-8").write(src)
    cp = subprocess.run([CJC, pr, "--diagnostic-format=json", "--output-type=staticlib", "-o", os.path.join(OUT, f"x_{tag}.out")],
                        capture_output=True, cwd=OUT, text=True, encoding="utf-8", errors="replace")
    raw = (cp.stdout or "") + (cp.stderr or "")
    try:
        data = json.loads(raw[raw.index("{"):]); errs = [d for d in data.get("Diags", []) if d.get("Severity") == "error"]
    except Exception:
        errs = [{"DiagKind": "PARSE_FAIL"}]
    print(f"{tag:16s} exit={cp.returncode}  " + (", ".join(sorted({d.get('DiagKind','?') for d in errs})) or "(no error)"))
