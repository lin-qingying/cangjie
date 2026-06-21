


main(): Int64 {
    let a = B()
    a.foo()
    if (a.a == 6 && a.b == 4) {
        return 0
    }
    return 1
}

class B {
    public var a: Int32 = 5
    public var b: Int32 = 5
    public var array = [1, 2, 3]
    public func foo() {
        this.a++
        this.b--
        array[0] = array[0] + 1
    }
}
