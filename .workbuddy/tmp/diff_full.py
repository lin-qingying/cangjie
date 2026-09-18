import re, collections

OLD = r"C:/Users/lin17/AppData/Local/Temp/cfir_at3.log"    # 18:33-18:59 全量（我改之前）
NEW = r"C:/Users/lin17/AppData/Local/Temp/cc_full2.log"    # 20:59-21:41 全量（我改之后）

pat = re.compile(r"^\s*(.+?)\s*FAILED\s*$")

def load(path):
    out = collections.Counter()
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            if not line.rstrip().endswith("FAILED"):
                continue
            m = pat.match(line.rstrip())
            if not m:
                continue
            body = m.group(1)
            if " > " not in body:
                continue
            body = re.sub(r"\s+", " ", body).replace("()", "")
            # 去掉行首缩进/噪声
            body = re.sub(r"^[\s\u2502\u251c\u2514\u2500]+", "", body)
            out[body] += 1
    return out

old = load(OLD)
new = load(NEW)
print(f"old 唯一的失败用例: {len(old)} 条（去重后）")
print(f"new 唯一的失败用例: {len(new)} 条（去重后）")

fixed = set(old) - set(new)
regressed = set(new) - set(old)
print(f"\nFIXED={len(fixed)}  REGRESSED={len(regressed)}  UNCHANGED={len(set(old) & set(new))}")

print("\n===== 新增失败（相对 18:59 基线）=====")
for k in sorted(regressed):
    print("  +", k)
print("\n===== 修复（节选，共 %d）=====" % len(fixed))
for k in sorted(fixed)[:40]:
    print("  -", k)
