

open class A {}

class B <: A {}

class C {}

extend C {
    static func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!><A>(a: A) {}
}

extend C {
    static func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!><X>(a: X) {}
}
