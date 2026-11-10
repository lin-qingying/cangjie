#!/usr/bin/env bash
set -u
cd /tmp || exit 1
echo "--- fetch page ---"
curl -sSL -m 60 -A "Mozilla/5.0" https://cangjie-lang.cn/download/1.0.5 -o dl.html || { echo "page fetch failed"; exit 1; }
echo "html bytes: $(wc -c < dl.html)"
echo "--- candidate urls (linux-x64) ---"
grep -oE 'https?://[^"'"'"' <>()]*linux-x64[^"'"'"' <>()]*' dl.html | sort -u
echo "--- any tar.gz urls ---"
grep -oE 'https?://[^"'"'"' <>()]*\.tar\.gz' dl.html | sort -u | head -20
echo "--- urls containing v1/files ---"
grep -oE 'https?://[^"'"'"' <>()]*v1/files[^"'"'"' <>()]*' dl.html | sort -u | head -20
echo "--- relative paths with cangjie-sdk ---"
grep -oE '"[^"]*cangjie-sdk-linux-x64[^"]*"' dl.html | sort -u | head -10
