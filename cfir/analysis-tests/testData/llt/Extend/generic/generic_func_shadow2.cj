

class C<A> {
    func f<X>(a: A, b: X) {}
    static func g<X>(a: A, b: X) {}
}

extend<B> C<B> {
    func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!><Y>(a: B, b: Y) {}
    static func <!EXTEND_MEMBER_CANNOT_SHADOW!>g<!><Y>(c: B, d: Y) {}
}
