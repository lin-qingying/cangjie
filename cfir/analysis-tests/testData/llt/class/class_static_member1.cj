


open class A {
    public static var a: Int64 = 1
    public var b: Int64 = 2
    <!STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER!>p<!>ublic static var c: Int64 = a + <!USED_BEFORE_INITIALIZATION!>b<!>
}

main(): Int64 {
    return 0
}
