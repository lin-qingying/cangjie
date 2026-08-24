


class A {
    public var a = 0
    public static func foo1() {
        return 0
    }
}

extend A {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW, STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo1<!>() {
        return a
    }
}

extend A {
    public func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo1<!>(a: Int32) {
        return a
    }
}

main(): Int64 {
    return 0
}
