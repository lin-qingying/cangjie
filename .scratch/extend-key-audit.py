# -*- coding: utf-8 -*-
"""Audit LLExtendStructuralKeys full-key uniqueness over decompiled builtins PSI."""
import glob
import os
import re
from collections import Counter, defaultdict

ROOT = r"D:\code\intellij\cangjie\analysis\stubs\testData\builtins"

extend_start = re.compile(r"^extend\s+(.+?)\s*(?:<:\s*(.*?))?\s*\{?\s*$")
member_re = re.compile(r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*((?:(?:public|private|protected|internal|static|open|override|mut|unsafe|foreign|const|operator|redef|abstract|sealed|common|specific|default)\s+)*)(func|prop|var|let|init|class|interface|struct|enum|typealias|macro)\b\s*([^\s(<:={]*)")

def short(name):
    name = name.strip()
    if name.startswith("?"):
        return "Option"
    name = name.split(".")[-1]
    name = name.split("<")[0]
    return name.strip()

def count_params(text):
    # text starts right after the name (may include <...> then (...))
    depth_angle = 0
    i = 0
    while i < len(text):
        c = text[i]
        if c == "<":
            depth_angle += 1
        elif c == ">":
            depth_angle -= 1
        elif c == "(" and depth_angle == 0:
            break
        i += 1
    if i >= len(text):
        return None
    depth = 0
    commas = 0
    content = ""
    j = i
    while j < len(text):
        c = text[j]
        if c in "([{":
            depth += 1
            if depth == 1 and c == "(":
                j += 1
                continue
        elif c in ")]}":
            depth -= 1
            if depth == 0:
                break
        if depth >= 1:
            content += c
        j += 1
    content = content.strip()
    if not content:
        return 0
    d = 0
    for c in content:
        if c in "([{":
            d += 1
        elif c in ")]}":
            d -= 1
        elif c == "," and d == 0:
            commas += 1
    return commas + 1

extend_key = re.compile(r"^extend\s")
records = []  # (package, file, receiver, supers, members, unknown)
for path in sorted(glob.glob(os.path.join(ROOT, "*.cjo.decompiled.text.cj"))):
    package = os.path.basename(path).replace(".cjo.decompiled.text.cj", "")
    cur = None
    with open(path, encoding="utf-8", errors="replace") as f:
        for lineno, raw in enumerate(f, 1):
            line = raw.rstrip("\n")
            if line.startswith("extend "):
                if cur:
                    records.append(cur)
                body = line[len("extend "):].strip()
                if body.endswith("{"):
                    body = body[:-1].strip()
                if "<:" in body:
                    recv, sup = body.split("<:", 1)
                    supers = [short(s) for s in sup.split(",") if short(s)]
                else:
                    recv, supers = body, []
                cur = {
                    "pkg": package,
                    "file": os.path.basename(path),
                    "line": lineno,
                    "recv": short(recv),
                    "supers": supers,
                    "members": [],
                    "unknown": [],
                }
                continue
            if cur is None:
                continue
            if line.startswith("}"):
                records.append(cur)
                cur = None
                continue
            if not line.strip() or line.strip().startswith("//"):
                continue
            m = member_re.search(line)
            if m:
                decl = m.group(2)
                name = m.group(3)
                rest = line[m.end():]
                arity = count_params(rest)
                if decl in ("var", "let"):
                    kind = "field"
                elif decl == "init":
                    kind = "init"
                else:
                    kind = decl
                cur["members"].append("%s|%s|%s" % (kind, name, arity))
            else:
                cur["unknown"].append((lineno, line.strip()))
    if cur:
        records.append(cur)

def full_key(r):
    return "%s<%s>|%s" % (r["recv"], ",".join(sorted(r["supers"])), "&".join(sorted(r["members"])))

print("total extends parsed:", len(records))
print("extends with unparsed member lines:", sum(1 for r in records if r["unknown"]))
for r in records:
    if r["unknown"]:
        print("  UNKNOWN", r["file"], r["line"], r["recv"], r["unknown"][:4])

by_pkg_key = defaultdict(list)
by_key = defaultdict(list)
for r in records:
    k = full_key(r)
    by_pkg_key[(r["pkg"], k)].append(r)
    by_key[k].append(r)

print("\n== same-package duplicate full keys (materializer deque collisions) ==")
dup_same = 0
for (pkg, k), rs in sorted(by_pkg_key.items()):
    if len(rs) > 1:
        dup_same += 1
        print("  %s  x%d  %s" % (pkg, len(rs), k[:200]))
print("count:", dup_same)

print("\n== cross-package duplicate full keys (global dedupe would drop) ==")
dup_cross = 0
for k, rs in sorted(by_key.items()):
    pkgs = sorted({r["pkg"] for r in rs})
    if len(pkgs) > 1:
        dup_cross += 1
        print("  x%d pkgs=%s  %s" % (len(rs), ",".join(pkgs), k[:220]))
print("count:", dup_cross)

print("\n== empty-member extends ==")
empty = [r for r in records if not r["members"]]
print("count:", len(empty))
for r in empty:
    print("  %s:%s %s" % (r["file"], r["line"], r["recv"]))
