


open class Father {
    protected open func f(): This {
        this
    }
    public open func f(a: Int64) {
        a
    }
    static func g(a: Int32) {
        a
    }
}

class Child1 <: Father {
    protected override func f() {
        this
    }
    public override func f(a: Int64) {
        a + 1
    }
    static redef func g(a: Int32) {
        a + 1
    }
}

class Child2 <: Father {
    public func f() {
        this
    }
    public func f(a: Int64) {
        a + 1
    }
    static func g(a: Int32) {
        a + 1
    }
}

main() {
    return 0
}
