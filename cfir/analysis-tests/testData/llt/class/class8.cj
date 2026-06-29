

open class A {
    public init() {}
    public init(a: Int64) {
        this()
        this.a = a
    }
    public func test() {
        var res = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>this<!>(1)
    }
    public var a: Int64 = 1
    public var b = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>this<!>()
}
