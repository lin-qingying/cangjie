

open class B {}

class A <: B {
    public var a = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>()
}

main() {
    A()
    C()
    0
}

class C {
    public var a = <!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>()
}
