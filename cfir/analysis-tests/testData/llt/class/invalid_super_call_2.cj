

open class B {}

class A <: B {
    public var a = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>()
}

main() {
    A()
    C()
    0
}

class C {
    public var a = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>()
}
