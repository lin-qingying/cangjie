

class A {
    public var x: Int64 = 0
    public init(x: Int64) {
        this.x = x
    }
}

extend A {
    public func foo(y: Int64) {
        return x - y
    }
}

main(): Int64 {
    let funcPtr = A(5).foo
    return funcPtr(5)
}
