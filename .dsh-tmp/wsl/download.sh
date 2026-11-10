#!/usr/bin/env bash
set -eu
cd "$HOME"
URL="https://gitcode.com/Cangjie/nightly_build/releases/download/1.3.0-alpha.20261004001050/cangjie-sdk-linux-x64-1.3.0-alpha.20261004001050.tar.gz"
echo "url: $URL"
curl -sSL --retry 3 --retry-delay 3 -o cj-sdk.tar.gz "$URL"
ls -l cj-sdk.tar.gz
rm -rf "$HOME/cj" && mkdir -p "$HOME/cj"
tar -xzf cj-sdk.tar.gz -C "$HOME/cj"
echo "--- extracted ---"
ls -l "$HOME/cj"
find "$HOME/cj" -maxdepth 3 -name envsetup.sh -print | head -5
echo "DL_AND_EXTRACT_DONE"
