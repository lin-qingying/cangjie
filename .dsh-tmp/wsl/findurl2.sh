#!/usr/bin/env bash
set -u
cd /tmp || exit 1
echo "--- context around linux-x64 (first 3) ---"
grep -oE '.{300}cangjie-sdk-linux-x64-1\.0\.5\.tar\.gz.{300}' dl.html | head -3
echo
echo "--- script srcs ---"
grep -oE '<script[^>]*src="[^"]+"' dl.html | sed 's/.*src="//; s/"$//' | sort -u
echo
echo "--- inline hints: api / files / download ---"
grep -oE '(/v1/[a-zA-Z0-9/_.-]+|https?://[^"'"'"' ]*(api|files|download)[^"'"'"' ]*)' dl.html | sort -u | head -40
