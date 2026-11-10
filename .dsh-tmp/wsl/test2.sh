#!/usr/bin/env bash
set -u
echo "=== sudo check ==="
if sudo -n true 2>/dev/null; then
  echo "SUDO_OK (passwordless)"
  export DEBIAN_FRONTEND=noninteractive
  sudo -n apt-get update -qq 2>&1 | tail -3
  sudo -n apt-get install -y -qq libc6-dev 2>&1 | tail -5
  echo "install_exit=$?"
else
  echo "SUDO_NEEDS_PASSWORD"
fi
echo "=== crt files ==="
ls -l /usr/lib/x86_64-linux-gnu/Scrt1.o /usr/lib/x86_64-linux-gnu/crti.o 2>&1 | head -4
echo "=== rebuild hello ==="
source "$HOME/cj/cangjie/envsetup.sh"
cd "$HOME/t" || exit 1
rm -f hello
cjc hello.cj -o hello 2>&1 | tail -6
if [ -x ./hello ]; then ./hello; echo "hello_run_exit=$?"; fi
echo "=== macro full output ==="
rm -f macro
cjc macro.cj -o macro 2>&1
echo "macro_compile_exit=$?"
if [ -x ./macro ]; then ./macro; echo "macro_run_exit=$?"; fi
