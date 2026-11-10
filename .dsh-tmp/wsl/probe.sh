#!/usr/bin/env bash
set -u
echo "whoami: $(whoami)"
echo "kernel: $(uname -r)"
echo "arch:   $(uname -m)"
. /etc/os-release 2>/dev/null || true
echo "os:     ${PRETTY_NAME:-unknown}"
echo "home:   $HOME"
echo "--- tools ---"
for t in curl wget tar unzip git gcc ld python3 sudo; do
  printf '%-8s %s\n' "$t" "$(command -v "$t" || echo MISSING)"
done
echo "--- disk ---"
df -h "$HOME" | tail -1
echo "--- net (12s timeout each) ---"
for u in https://cangjie-lang.cn https://gitcode.com https://mirrors.tuna.tsinghua.edu.cn; do
  code=$(curl -sS -m 12 -o /dev/null -w '%{http_code}' "$u" 2>/dev/null || echo ERR)
  echo "$u -> $code"
done
