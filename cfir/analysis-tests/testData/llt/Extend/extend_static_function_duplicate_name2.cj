


class A {
    public var a = 0
    public func foo1() {
        return 0
    }
}

extend A {
    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic static func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo1<!>() {
        return 0
    }
}

extend A {
    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic static func foo1(a: Int32) {
        return 0
    }
}

main(): Int64 {
    return 0
}
