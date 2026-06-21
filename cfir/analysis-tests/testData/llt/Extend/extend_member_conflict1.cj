

class A {}

extend A {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>() {}
}

extend A {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>() {}
}
