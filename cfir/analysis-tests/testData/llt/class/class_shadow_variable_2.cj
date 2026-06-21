


open class A {
    public var a = 1
}

open class B <: A {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var a = 10
}

class C <: B {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var a = 20
}

main() {
    return 1
}
