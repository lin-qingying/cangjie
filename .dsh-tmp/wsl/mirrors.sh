#!/usr/bin/env bash
set -u
probe() { printf '%-72s ' "$1"; curl -sS -m 25 -o /tmp/p.out -w '%{http_code} %{size_download}B\n' -L -A "Mozilla/5.0" "$1" || echo " ERR"; }

echo "=== gitcode api: nightly releases ==="
probe "https://api.gitcode.com/api/v5/repos/Cangjie/nightly_build/releases?per_page=3"
head -c 600 /tmp/p.out; echo; echo

echo "=== gitcode releases page ==="
probe "https://gitcode.com/Cangjie/nightly_build/releases"
grep -oE 'https://[^"'"'"' ]*(tar\.gz|\.zip)' /tmp/p.out | sort -u | head -20

echo "=== huawei mirror candidates ==="
for u in https://mirrors.huaweicloud.com/cangjie/ https://repo.huaweicloud.com/cangjie/ https://mirrors.tuna.tsinghua.edu.cn/cangjie/ https://mirrors.ustc.edu.cn/cangjie/; do
  printf '%-60s ' "$u"; curl -sS -m 20 -o /dev/null -w '%{http_code}\n' -L "$u" || echo ERR
done

echo "=== direct cdn guesses ==="
for u in \
 https://cangjie-lang.cn/v1/files/cangjie-sdk-linux-x64-1.0.5.tar.gz \
 https://cangjie-lang.cn/downloads/cangjie-sdk-linux-x64-1.0.5.tar.gz ; do
  printf '%-78s ' "$u"; curl -sS -m 20 -o /dev/null -w '%{http_code} %{content_type}\n' -L "$u" || echo ERR
done
