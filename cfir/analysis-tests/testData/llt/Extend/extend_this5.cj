


extend Int64 {
    public static func foo() {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>this<!>
    }
}

class A {
    public func f() {}
}

extend A {
    public static func foo() {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>this<!>.f()
    }
}

struct B {
    public let item: Int64 = 0
}

extend B {
    public static func foo() {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>this<!>.item
    }
}
