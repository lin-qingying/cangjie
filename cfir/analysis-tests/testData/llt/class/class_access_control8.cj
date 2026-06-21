


open class A {
    public var a: Int32 = 1
    protected var d: Int32 = 2
}

class B <: A {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>rivate var d: Int32 = 1
}

main(): Int64 {
    return 0
}
