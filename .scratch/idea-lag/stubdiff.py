import io, os, re
root = r"D:\code\intellij\cangjie"
java = os.path.join(root, r"psi\src\org\cangnova\cangjie\psi\stubs\elements\CjStubElementTypes.java")
text = io.open(java, encoding="utf-8", errors="replace").read()
declared = sorted({m.group(1) for m in re.finditer(r"\b([A-Z][A-Z0-9_]{2,})\s*=", text)})

def used(d):
    s = set()
    for base, _, files in os.walk(d):
        for f in files:
            if not f.endswith((".kt", ".java")):
                continue
            t = io.open(os.path.join(base, f), encoding="utf-8", errors="replace").read()
            s |= {m.group(1) for m in re.finditer(r"CjStubElementTypes\.([A-Z][A-Z0-9_]+)", t)}
    return s

dec = used(os.path.join(root, "analysis", "decompiled"))
psi = used(os.path.join(root, r"psi\src"))

print(f"declared={len(declared)} decompiled-used={len(dec)} psi-used={len(psi)}")
missing = [x for x in declared if x not in dec]
print(f"\n=== 反编译侧从未创建/引用的 stub 类型 ({len(missing)}) ===")
for x in missing:
    tag = "parser用" if x in psi else "?"
    print(f"  {x:35s} {tag}")
print(f"\n=== 反编译侧引用但未在 CjStubElementTypes 常量里找到 ===")
for x in sorted(dec - set(declared)):
    print("  ", x)
