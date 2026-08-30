

open class A {
    public var a: Int32 = 1
    private var b: Int32 = a
    protected var d: Int32 = 2
    public var c: Int32 = d
}

class B <: A {
    private var e = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>a<!>
    private var g = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>d<!>
    private var h = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>c<!>

    public var i = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>a<!>
    public var k = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>d<!>
    public var l = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>c<!>

    protected var m = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>a<!>
    protected var o = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>d<!>
    protected var p = <!ILLEGAL_USAGE_OF_SUPER_MEMBER!>c<!>

    public func f() {
        if (e == 1 && g == 2 && h == 2 && i == 1 && k == 2 && l == 2 && m == 1 && o == 2 && p == 2) {
            return 0
        } else {
            return 1
        }
    }
}

main(): Int64 {
    var testB = B()
    testB.f()
    return 0
}
