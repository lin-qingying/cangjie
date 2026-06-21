

class A {
    static const zero = 0
    const func <!NO_CONST_INIT!>f<!>() {
        zero
    }
}

main() {
    let a = A()
    a.f()
}
