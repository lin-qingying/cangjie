


class A {
    public var a = 0
    public func foo1() {
        return 0
    }
}

extend A {
    public static func <!EXTEND_MEMBER_CANNOT_SHADOW, STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo1<!>() {
        return 0
    }
}

extend A {
    public static func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo1<!>(a: Int32) {
        return 0
    }
}

main(): Int64 {
    return 0
}
