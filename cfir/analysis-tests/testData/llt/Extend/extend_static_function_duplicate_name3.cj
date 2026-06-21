

class A {
    public var a = 0
    public static func foo1() {
        return 0
    }
}

extend A {
    public static func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo1<!>() {
        return 0
    }
}

extend A {
    public static func foo1(a: Int32) {
        return 0
    }
}

main(): Int64 {
    return 0
}
