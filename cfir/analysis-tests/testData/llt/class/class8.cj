

open class A {
    public init() {}
    public init(a: Int64) {
        this()
        this.a = a
    }
    public func test() {
        var res = <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>(1)
    }
    public var a: Int64 = 1
    public var b = <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>()
}
