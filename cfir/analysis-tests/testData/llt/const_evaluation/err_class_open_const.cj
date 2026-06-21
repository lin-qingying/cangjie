

class A {
    // const member function cannot be open
    <!INCOMPATIBLE_MODIFIERS!>open<!> <!INCOMPATIBLE_MODIFIERS!>const<!> func f() { 0 }
    const init() {}
}

main() {
    let a = A()
    a.f()
}
