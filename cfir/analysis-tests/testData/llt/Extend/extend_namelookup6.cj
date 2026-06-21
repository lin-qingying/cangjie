

interface I {
    func foo() {}
}

class A {}

extend A <: I {}

main() {
    let a = A()
    a.foo()
    0
}
