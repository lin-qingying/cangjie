

struct S {
    <!INCOMPATIBLE_MODIFIERS!>mut<!> <!INCOMPATIBLE_MODIFIERS!>const<!> func f() { 0 }
    const init() {}
}

main() {
    let s = S()
    s.f()
}
