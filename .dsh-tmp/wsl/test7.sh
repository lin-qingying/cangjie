#!/usr/bin/env bash
set -u
source "$HOME/cj/cangjie/envsetup.sh"
SYS="$HOME/sysroot"
CRT="$SYS/usr/lib/x86_64-linux-gnu"
GCCDIR="$SYS/usr/lib/gcc/x86_64-linux-gnu/15"
echo "=== files in crt dir with GROUP refs ==="
grep -l 'GROUP' "$CRT"/*.so 2>/dev/null | head
for f in "$CRT"/*.so; do
  [ -f "$f" ] || continue
  sed -i "s#/usr/lib/x86_64-linux-gnu/libc_nonshared.a#$CRT/libc_nonshared.a#g; s#/usr/lib/x86_64-linux-gnu/libmvec.a#$CRT/libmvec.a#g" "$f"
done
echo "=== libc.so now ==="
cat "$CRT/libc.so" 2>/dev/null | tail -3
echo "=== existence check ==="
ls -l "$CRT/libc_nonshared.a" "$CRT/libc.so.6" 2>&1 | head -3

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
