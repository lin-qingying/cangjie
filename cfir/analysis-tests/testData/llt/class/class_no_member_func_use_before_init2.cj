

class A {
    public var a = <!ILLEGAL_USAGE_OF_MEMBER!>f<!>
    public var b: () -> Int64
    public var d = g
    public static var e = g
    public init(x: () -> Int64) {
        func g() {
            0
        }
        b = g
        a = x
    }
    public init() {
        b = g
        var h = f()
        a = f
        a = this.f
    }
    public func f(): Int64 {
        return 0
    }
    public static func g(): Int64 {
        return 1
    }
}
