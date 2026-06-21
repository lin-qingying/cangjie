


class B {
    public var i: Int64 = 2
    public func foo(): Int64 {
        this.i++
        return this.i - 3
    }
}

main(): Int64 {
    var b = B()
    return b.foo()
}
