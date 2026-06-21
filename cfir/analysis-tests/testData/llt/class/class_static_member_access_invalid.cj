


open class A {
    public static var a: Int64 = 1
}

class B <: A {}

main(): Int64 {
    var b: Int64 = <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>B<!>().a
    B.a
    return 0
}
