

interface I {
    const func f(): Int64
}

class A {}

extend A <: I {
    public const func <!NO_CONST_INIT!>f<!>() {
        0
    }
}

main() {
    let a = A()
    a.f()
}
