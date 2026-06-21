

class A {}

extend A {
    public static func <!STATIC_FUNCTION_OVERLOAD_CONFLICTS!>f<!>(a: Int8) {}
    public func f() {}
}
