#!/usr/bin/env python3
"""Validate Conventional Commit subjects for a PR or push range."""
import argparse
import re
import subprocess
import sys

PATTERN = re.compile(
    r"^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)"
    r"(\([a-zA-Z0-9._/-]+\))?(!)?: .+"
)

def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    parser.add_argument("--pr", action="store_true")
    args = parser.parse_args()
    if args.pr:
        revision = f"{git('merge-base', args.base, args.head)}..{args.head}"
    elif set(args.base) == {"0"}:
        revision = args.head
    else:
        revision = f"{args.base}..{args.head}"
    commits = git("rev-list", "--no-merges", revision).splitlines()
    invalid = []
    for sha in commits:
        subject = git("show", "-s", "--format=%s", sha)
        if not PATTERN.fullmatch(subject):
            invalid.append((sha[:12], subject))
    if invalid:
        print("Invalid Conventional Commit subjects:")
        for sha, subject in invalid:
            print(f"  {sha}: {subject}")
        print("Expected: type(scope)!: description, e.g. feat(cfir): add symbol resolution")
        return 1
    print(f"Validated {len(commits)} commit subject(s).")
    return 0

if __name__ == "__main__":
    sys.exit(main())
