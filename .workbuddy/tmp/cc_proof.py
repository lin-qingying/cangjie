import os, re, subprocess, json
CJC = r"C:/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc.exe"
OUT = r"D:/code/intellij/cangjie/tmp/cc-verify"
marker = re.compile(r"<!>|<!([A-Z0-9_,\s]+)!>")
p = r"cfir/analysis-tests/testData/llt/class/class_no_override_modifier_invalid_5.cj"
src = marker.sub("", open(p, encoding="utf-8").read())
pr = os.path.join(OUT, "proof_invalid5.cj")
open(pr, "w", encoding="utf-8").write(src)
cp = subprocess.run([CJC, pr, "--diagnostic-format=json", "--output-type=staticlib", "-o", os.path.join(OUT, "proof_invalid5.out")],
                    capture_output=True, cwd=OUT, text=True, encoding="utf-8", errors="replace")
data = json.loads(((cp.stdout or "") + (cp.stderr or ""))[((cp.stdout or "") + (cp.stderr or "")).index("{"):])
print("--- fixture: class_no_override_modifier_invalid_5.cj ---")
for d in data.get("Diags", []):
    loc = d.get("Location") or {}
    print(f"  [{d.get('Severity')}] L{loc.get('Line')}:C{loc.get('Column')}  {d.get('DiagKind')}")
    print(f"      msg: {d.get('Message')}")
