


interface I {
    func coo() {}
}

open class A <: I {
    protected func foo() {}
}

extend A {
    func goo() {}
}

open class B <: A {
    public init() {
        { // lambda
            =>
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>oo()
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>g<!>oo()
            let f1 = <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>oo
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>g<!>oo
            this.<!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>c<!>oo
        }
    }
}

main(): Int64 {
    0
}
