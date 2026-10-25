import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
# class phase failure causes
idx = [i for i, l in enumerate(lines) if "19:51:34,479" in l and "diagnostic collector" in l][0]
print("=== class BODY_RESOLVE failure: causes ===")
for x in lines[idx:idx+90]:
    if "Caused by" in x or "expected" in x or "ResolvedTo" in x or "checkPhase" in x:
        print(x[:240])
print("\n=== cache inconsistency stack (tail) ===")
j = [i for i, l in enumerate(lines) if "19:51:35,492" in l and "Inconsistency in the cache" in l][0]
for x in lines[j:j+45]:
    print(x[:220])
