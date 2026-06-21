

class A {
    private func foo(): Int64 {
        return 1
    }
}

extend A {
    public func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    let a = A()
    return a.foo() - 2
}
