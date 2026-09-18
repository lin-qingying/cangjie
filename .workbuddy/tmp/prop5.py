import os, re, subprocess, json
CJC = r"C:/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc.exe"
OUT = r"D:/code/intellij/cangjie/tmp/cc-verify"
marker = re.compile(r"<!>|<!([A-Z0-9_,\s]+)!>")
p = r"cfir/analysis-tests/testData/llt/interface/interface_property/interface_property5.cj"
src = marker.sub("", open(p, encoding="utf-8").read())
pr = os.path.join(OUT, "prop5.cj"); open(pr, "w", encoding="utf-8").write(src)
cp = subprocess.run([CJC, pr, "--diagnostic-format=json", "--output-type=staticlib", "-o", os.path.join(OUT, "prop5.out")],
                    capture_output=True, cwd=OUT, text=True, encoding="utf-8", errors="replace")
raw = (cp.stdout or "") + (cp.stderr or "")
data = json.loads(raw[raw.index("{"):])
print("官方诊断（按输出顺序）:")
for d in data.get("Diags", []):
    loc = d.get("Location") or {}
    print(f"  L{loc.get('Line')}:C{loc.get('Column')}  {d.get('DiagKind')}  -- {(d.get('Message') or '')[:70]}")
