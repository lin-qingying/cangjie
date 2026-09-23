"""Filter a unified diff down to the hunks that belong to the typealias batch.

Usage:
    python filter_hunks.py <diff-file> <out-patch> <keep-regex> [<drop-regex>]

A hunk is kept when its body matches <keep-regex> and (if given) does not match
<drop-regex>. Header lines are always preserved.
"""
import re
import sys


def main() -> int:
    src, dst, keep_re, drop_re = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None
    with open(src, encoding="utf-8", newline="") as fh:
        lines = fh.read().split("\n")

    keep = re.compile(keep_re)
    drop = re.compile(drop_re) if drop_re else None

    header: list[str] = []
    hunks: list[list[str]] = []
    current: list[str] | None = None
    for line in lines:
        if line.startswith("@@"):
            current = [line]
            hunks.append(current)
        elif current is not None:
            current.append(line)
        else:
            header.append(line)

    kept: list[list[str]] = []
    for hunk in hunks:
        body = "\n".join(hunk)
        if not keep.search(body):
            continue
        if drop is not None and drop.search(body):
            continue
        kept.append(hunk)

    out = [line for line in header if line != ""] + [line for hunk in kept for line in hunk if line != ""]
    with open(dst, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(out) + "\n")

    print(f"{src}: {len(hunks)} hunks -> kept {len(kept)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
