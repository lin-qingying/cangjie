

interface I {
    func foo(): Int64
}

class A {}

extend A <: I {
    public func foo(): Int64 {
        return 0
    }
}

main() {
    let a: I = A()
    return a.foo()
}
