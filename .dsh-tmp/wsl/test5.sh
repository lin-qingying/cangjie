#!/usr/bin/env bash
set -u
source "$HOME/cj/cangjie/envsetup.sh"
SYS="$HOME/sysroot"
CRT="$SYS/usr/lib/x86_64-linux-gnu"
GCCDIR="$SYS/usr/lib/gcc/x86_64-linux-gnu/15"
cd "$HOME/t" || exit 1
export LIBRARY_PATH="$CRT:$GCCDIR:${LIBRARY_PATH:-}"
echo "=== crt files in sysroot ==="
ls -1 "$CRT"/[Sc][c]rt*.o 2>/dev/null | head -10
ls -1 "$GCCDIR"/crt*.o 2>/dev/null | head -10
# ld resolves bare input names against cwd, so link them into the project dir
for f in Scrt1.o crt1.o crti.o crtn.o crtbeginS.o crtendS.o crtbegin.o crtend.o; do
  for src in "$CRT/$f" "$GCCDIR/$f"; do
    [ -f "$src" ] && ln -sf "$src" "./$f"
  done
done
ls -l ./Scrt1.o ./crti.o ./crtbeginS.o 2>&1 | head -5

echo "=== hello ==="
rm -f hello
cjc hello.cj -o hello 2>&1 | tail -5
[ -x ./hello ] && { ./hello; echo "hello_run_exit=$?"; }

echo "=== macro end-to-end ==="
rm -f macro
cjc macro.cj -o macro 2>&1 | tail -8
if [ -x ./macro ]; then ./macro; echo "macro_run_exit=$?"; else echo "macro not built"; fi
