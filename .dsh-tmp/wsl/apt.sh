#!/usr/bin/env bash
set -u
cd "$HOME/debs" || exit 1
echo "=== try plain download ==="
apt-get download libc6-dev 2>&1 | tail -5
echo "exit=$?"
echo "=== print-uris ==="
apt-get download --print-uris libc6-dev 2>&1 | head -3
echo "=== download-only into private cache ==="
mkdir -p "$HOME/aptcache/archives/partial"
apt-get -o Dir::Cache="$HOME/aptcache" -o Debug::NoLocking=1 install --download-only -y libc6-dev 2>&1 | tail -6
ls -l "$HOME/aptcache/archives"/*.deb 2>/dev/null | head -5
