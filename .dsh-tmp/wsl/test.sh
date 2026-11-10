#!/usr/bin/env bash
set -u
source "$HOME/cj/cangjie/envsetup.sh"
echo "CANGJIE_HOME=${CANGJIE_HOME:-unset}"
echo "cjc: $(command -v cjc || echo MISSING)"
cjc --version 2>&1 | head -3
echo "cjpm: $(command -v cjpm || echo MISSING)"
cjpm --version 2>&1 | head -3
echo "=== hello world ==="
rm -rf "$HOME/t" && mkdir -p "$HOME/t" && cd "$HOME/t" || exit 1
cat > hello.cj <<'EOF'
main() {
    println("hello from linux cangjie")
}
EOF
cjc hello.cj -o hello 2>&1 | tail -5
echo "compile_exit=$?"
if [ -x ./hello ]; then ./hello; echo "run_exit=$?"; fi
echo "=== macro expansion (@Derive) ==="
cat > macro.cj <<'EOF'
import std.deriving.*

@Derive[ToString]
struct Point {
    let x: Int64
    let y: Int64
}

main() {
    println(Point(1, 2).toString())
}
EOF
cjc macro.cj -o macro 2>&1 | tail -10
echo "macro_compile_exit=$?"
if [ -x ./macro ]; then ./macro; echo "macro_run_exit=$?"; fi
