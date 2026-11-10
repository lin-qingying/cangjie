#!/usr/bin/env bash
set -u
export APTDIRS="-o Dir::State=$HOME/aptstate -o Dir::State::lists=$HOME/aptstate/lists -o Dir::Cache=$HOME/aptcache -o Debug::NoLocking=1"
mkdir -p "$HOME/aptstate/lists/partial" "$HOME/aptcache/archives/partial"
SRC=$(ls /etc/apt/sources.list.d/*.sources 2>/dev/null | head -1)
[ -z "$SRC" ] && SRC=/etc/apt/sources.list
echo "sources: $SRC"
timeout 200 apt-get $APTDIRS -o Dir::Etc::sourcelist="$SRC" -o Dir::Etc::sourceparts="-" update 2>&1 | tail -4

cd "$HOME/debs" || exit 1
timeout 120 apt-get $APTDIRS download libc6-dev libgcc-15-dev 2>&1 | tail -6
ls -1 *.deb | head -6

mkdir -p "$HOME/sysroot"
for d in *.deb; do dpkg-deb -x "$d" "$HOME/sysroot" >/dev/null 2>&1; done
CRT=$(dirname "$(find "$HOME/sysroot" -name Scrt1.o | head -1)" 2>/dev/null)
GCCDIR=$(dirname "$(find "$HOME/sysroot" -name crtbeginS.o | head -1)" 2>/dev/null)
echo "crt=$CRT"
echo "gccdir=$GCCDIR"

source "$HOME/cj/cangjie/envsetup.sh"
export LIBRARY_PATH="$CRT:$GCCDIR:${LIBRARY_PATH:-}"
cd "$HOME/t" || exit 1
echo "=== hello ==="
rm -f hello
cjc hello.cj -o hello 2>&1 | tail -4
[ -x ./hello ] && { ./hello; echo "hello_run_exit=$?"; }
echo "=== macro ==="
rm -f macro
cjc macro.cj -o macro 2>&1 | tail -6
if [ -x ./macro ]; then ./macro; echo "macro_run_exit=$?"; else echo "macro not built"; fi
