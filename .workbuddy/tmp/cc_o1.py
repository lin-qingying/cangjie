import os, subprocess, json

CJC = r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe"
OUT = r"D:\code\intellij\cangjie\tmp\cc-verify"

PRELUDE = """
func g<T>(x: T, y: T): T where T <: Equatable<T> { x }

class A <: Equatable<A> {
    public operator func !=(_: A) { false }
    public operator func ==(_: A) { true }
}
"""

PROBES = {
    "o_baseline": PRELUDE + "\nmain() {\n    let _ = g(1, Some(1))\n}\n",
    "o_expect_option": PRELUDE + "\nmain() {\n    let a: Option<Int64> = g(1, Some(1))\n}\n",
    "o_expect_int": PRELUDE + "\nmain() {\n    let a: Int64 = g(1, Some(1))\n}\n",
    "o_expect_any": PRELUDE + "\nmain() {\n    let a: Any = g(1, Some(1))\n}\n",
    "o_class_opt_expect_option": PRELUDE + "\nmain() {\n    let a: Option<A> = g(A(), Some(A()))\n}\n",
    "o_class_opt_expect_a": PRELUDE + "\nmain() {\n    let a: A = g(A(), Some(A()))\n}\n",
    "o_no_constraint": """
func h<T>(x: T, y: T): T { x }

main() {
    let a: Option<Int64> = h(1, Some(1))
}
""",
    "o_three_args": """
func k<T>(x: T, y: T, z: T): T { x }

main() {
    let a: Option<Int64> = k(1, 2, Some(1))
}
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
    errs = []
    try:
        data = json.loads(raw[raw.index("{"):])
        errs = [d for d in data.get("Diags", []) if d.get("Severity") == "error"]
    except Exception:
        errs = [{"DiagKind": "PARSE_FAIL", "Message": raw[:120]}]
    print(f"{tag:28s} exit={cp.returncode}  errors={len(errs)}  " +
          ", ".join(sorted({d.get('DiagKind', '?') for d in errs})))
