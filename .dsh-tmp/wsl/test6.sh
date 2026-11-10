#!/usr/bin/env bash
set -u
source "$HOME/cj/cangjie/envsetup.sh"
SYS="$HOME/sysroot"
CRT="$SYS/usr/lib/x86_64-linux-gnu"
GCCDIR="$SYS/usr/lib/gcc/x86_64-linux-gnu/15"
echo "=== rewrite absolute paths inside linker scripts ==="
for f in "$CRT"/*.so "$CRT"/*.a; do
  case "$(head -c 4 "$f" 2>/dev/null)" in
    "/*") sed -i "s#/usr/lib/x86_64-linux-gnu#$CRT#g" "$f"; echo "patched $(basename "$f")";;
  esac
done
head -c 400 "$CRT/libc.so" 2>/dev/null; echo

cd "$HOME/t" || exit 1
export LIBRARY_PATH="$CRT:$GCCDIR:${LIBRARY_PATH:-}"
for f in Scrt1.o crt1.o crti.o crtn.o crtbeginS.o crtendS.o crtbegin.o crtend.o; do
  for src in "$CRT/$f" "$GCCDIR/$f"; do [ -f "$src" ] && ln -sf "$src" "./$f"; done
done

echo "=== hello ==="
rm -f hello
cjc hello.cj -o hello 2>&1 | tail -4
[ -x ./hello ] && { ./hello; echo "hello_run_exit=$?"; }

echo "=== macro end-to-end ==="
rm -f macro
cjc macro.cj -o macro 2>&1 | tail -8
if [ -x ./macro ]; then ./macro; echo "macro_run_exit=$?"; else echo "macro not built"; fi
