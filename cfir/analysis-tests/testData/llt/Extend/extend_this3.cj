

class A {
    public let x: Int64 = 1
}

extend A {
    public func foo1(): A {
        return this
    }
    public func foo2(): Int64 {
        return foo1().x - 1
    }
}

main() {
    return A().foo2()
}
