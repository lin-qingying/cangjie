


open class A {
    public var a: Int32 = 1
    public let d: Int32 = 2
}

class B <: A {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>rivate var a: Int32 = 2
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>rotected let d: Int32 = 3
}

main(): Int64 {
    return 0
}
