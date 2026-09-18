import os, re, subprocess, json
CJC = r"C:/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc.exe"
OUT = r"D:/code/intellij/cangjie/tmp/cc-verify"
BASE = r"D:/code/intellij/cangjie"
marker = re.compile(r"<!>|<!([A-Z0-9_,\s]+)!>")

def probe(tag, rel):
    p = os.path.join(BASE, rel)
    src = marker.sub("", open(p, encoding="utf-8").read())
    d = os.path.join(OUT, f"m_{tag}")
    os.makedirs(d, exist_ok=True)
    # 多文件 fixture 按 // FILE: 拆分
    parts = re.split(r"^\s*//\s*FILE:/s*(/S+)/s*$", src, flags=re.M)
    files = []
    if len(parts) > 1:
        for i in range(1, len(parts), 2):
            name = os.path.basename(parts[i]); body = parts[i + 1]
            fp = os.path.join(d, name); open(fp, "w", encoding="utf-8").write(body); files.append(fp)
    else:
        fp = os.path.join(d, f"{tag}.cj"); open(fp, "w", encoding="utf-8").write(src); files.append(fp)
    cp = subprocess.run([CJC] + files + ["--diagnostic-format=json", "--output-type=staticlib", "-o", os.path.join(d, "out")],
                        capture_output=True, cwd=d, text=True, encoding="utf-8", errors="replace")
    raw = (cp.stdout or "") + (cp.stderr or "")
    try:
        data = json.loads(raw[raw.index("{"):]); errs = [x for x in data.get("Diags", []) if x.get("Severity") == "error"]
    except Exception:
        errs = [{"DiagKind": "PARSE_FAIL"}]
    print(f"{tag:14s} files={len(files)} exit={cp.returncode}  " + (", ".join(sorted({x.get('DiagKind','?') for x in errs})) or "(no error)"))

probe("invalid9", r"cfir/analysis-tests/testData/llt/class/class_no_override_modifier_invalid_9/class_no_override_modifier_invalid_9.cj")
probe("prop5", r"cfir/analysis-tests/testData/llt/interface/interface_property/interface_property5.cj")
