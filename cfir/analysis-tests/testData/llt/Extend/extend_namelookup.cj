

class A {
    func go(): Int64 {
        return 0
    }
}

extend A {
    func foo(): Int64 {
        go()
    }
}

main(): Int64 {
    let a = A()
    return a.foo()
}
