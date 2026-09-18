import os, re, subprocess, json

CJC = r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe"
OUT = r"D:\code\intellij\cangjie\tmp\cc-verify"

PROBES = {
    "iface_no_impl": """
interface I {
    func f(): Unit
}

class A <: I {}
""",
    "iface_default_body": """
interface I {
    func f(): Unit { return }
}

class A <: I {}
""",
    "abstract_class_member": """
abstract class B {
    public abstract func g(): Unit
}

class C <: B {}
""",
    "struct_iface_no_impl": """
interface I {
    func f(): Unit
}

struct A <: I {}
""",
}

for tag, src in PROBES.items():
    probe = os.path.join(OUT, f"{tag}.cj")
    with open(probe, "w", encoding="utf-8") as f:
        f.write(src)
    cp = subprocess.run([CJC, probe, "--diagnostic-format=json", "--output-type=staticlib",
                         "-o", os.path.join(OUT, f"{tag}.out")],
                        capture_output=True, cwd=OUT, text=True, encoding="utf-8", errors="replace")
    raw = (cp.stdout or "") + (cp.stderr or "")
    print("=" * 90)
    print(f"### {tag}  exit={cp.returncode}")
    try:
        data = json.loads(raw[raw.index("{"):])
    except Exception as e:
        print("  parse fail:", raw[:300]); continue
    for d in data.get("Diags", []):
        loc = d.get("Location") or {}
        print(f"  [{d.get('Severity')}] L{loc.get('Line')}:C{loc.get('Column')} "
              f"{d.get('DiagKind')} -- {(d.get('Message') or '')[:90]}")
