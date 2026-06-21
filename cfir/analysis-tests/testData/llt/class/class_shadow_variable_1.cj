


open class A {
    public var a = 1
}

class B <: A {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var a = 10
}

main() {
    return 1
}
