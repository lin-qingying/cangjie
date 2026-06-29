


func mm() {
    <!THIS_SUPER_USE_ERROR_OUTSIDE_CLASS!>super<!>()
    <!THIS_SUPER_USE_ERROR_OUTSIDE_CLASS!>super<!>
}

open class c {
    var c: Int64 = 1
}

class m <: c {
    public var a = <!ILLEGAL_SUPER_ALONE!>super<!> // ok
    public var b = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>()
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var c: Int64 = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>()
    public static var d = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>()
    public static var e: Int64 = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>()
    public init() {
        super() // ok
    }
    public init(a: Int64) {
        <!ILLEGAL_SUPER_ALONE!>super<!> // ok
    }
    public init(a: Int64, b: Int64) {
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>super<!>().<!NOT_MEMBER_OF!>a<!>
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>super<!>().c
    }
    public init(a: Int64, b: Int64, c: Int64) {
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>super<!>().c++
    }
    public func f() {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>()
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>().c
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>().c++
    }
    public static func ff() {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>()
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>().c
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>().c++
    }
}
