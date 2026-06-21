

class A {}

extend A {
    func go(): Int64 {
        return 0
    }
    func foo() {
        go()
    }
}

main(): Int64 {
    let a = A()
    return a.foo()
}
