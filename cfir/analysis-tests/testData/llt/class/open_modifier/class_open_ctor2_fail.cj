


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
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>foo<!>()
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>goo<!>()
            let f1 = <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>foo<!>
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>goo<!>
            this.<!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>coo<!>
        }
    }
}

main(): Int64 {
    0
}
