#!/usr/bin/env bash
set -u
for base in https://repo.huaweicloud.com/cangjie/ https://mirrors.huaweicloud.com/cangjie/; do
  echo "===== $base ====="
  curl -sSL -m 30 "$base" | grep -oE 'href="[^"]+"' | sed 's/href="//; s/"$//' | head -40
done
echo
echo "===== look for 1.0.5 dirs ====="
for u in https://repo.huaweicloud.com/cangjie/1.0.5/ https://repo.huaweicloud.com/cangjie/1.0.0/ ; do
  echo "--- $u"
  curl -sSL -m 30 "$u" | grep -oE 'href="[^"]+"' | sed 's/href="//; s/"$//' | head -30
done
