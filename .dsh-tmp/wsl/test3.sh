#!/usr/bin/env bash
set -u
source "$HOME/cj/cangjie/envsetup.sh"
cd "$HOME/t" || exit 1

# 1. fix the test program: Cangjie struct needs an explicit primary constructor
cat > macro.cj <<'EOF'
import std.deriving.*

@Derive[ToString]
struct Point {
    Point(let x: Int64, let y: Int64) {}
}

main() {
    println(Point(1, 2).toString())
}
EOF

# 2. fetch the glibc/gcc crt files without root
mkdir -p "$HOME/debs" "$HOME/sysroot"
cd "$HOME/debs" || exit 1
GCCDEV=$(apt-cache pkgnames 2>/dev/null | grep -E '^libgcc-[0-9]+-dev$' | head -1)
echo "gccdev=$GCCDEV"
apt-get download libc6-dev $GCCDEV >/dev/null 2>&1
ls -1 *.deb 2>/dev/null | head -5
for d in *.deb; do dpkg-deb -x "$d" "$HOME/sysroot" >/dev/null 2>&1; done
CRT=$(dirname "$(find "$HOME/sysroot" -name Scrt1.o | head -1)" 2>/dev/null)
GCCDIR=$(dirname "$(find "$HOME/sysroot" -name crtbeginS.o | head -1)" 2>/dev/null)
echo "crt=$CRT gccdir=$GCCDIR"

export LIBRARY_PATH="$CRT:$GCCDIR:${LIBRARY_PATH:-}"
cd "$HOME/t" || exit 1

echo "=== hello (with sysroot crt) ==="
rm -f hello
cjc hello.cj -o hello 2>&1 | tail -4
[ -x ./hello ] && { ./hello; echo "hello_run_exit=$?"; }

echo "=== macro end-to-end ==="
rm -f macro
cjc macro.cj -o macro 2>&1 | tail -20
if [ -x ./macro ]; then ./macro; echo "macro_run_exit=$?"; else echo "macro binary not produced"; fi
