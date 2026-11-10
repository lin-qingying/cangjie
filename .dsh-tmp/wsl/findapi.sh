#!/usr/bin/env bash
set -u
cd /tmp || exit 1
for u in \
  "https://csdnimg.cn/release/devpress-cangjie/public/js/chunk/organization/download/version.f405f0c6.js" \
  "https://csdnimg.cn/release/devpress-cangjie/public/js/chunk/common.bff2cd66.js" ; do
  f=$(basename "$u")
  curl -sSL -m 60 -A "Mozilla/5.0" "$u" -o "$f" && echo "got $f ($(wc -c < "$f") bytes)" || echo "fail $u"
done
echo "--- endpoint-ish strings ---"
grep -ohE '"/[a-zA-Z0-9/_.$?=&{}-]{4,80}"' version.f405f0c6.js common.bff2cd66.js 2>/dev/null | sort -u | head -60
echo "--- keywords ---"
grep -ohE '.{60}(downloadUrl|fileUrl|packageUrl|getDownload|downloadFile|/download/)[^,;]{0,80}' version.f405f0c6.js common.bff2cd66.js 2>/dev/null | head -20
