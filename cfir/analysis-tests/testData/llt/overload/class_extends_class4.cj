

open class Base {
    public func foo(a: Int64) {
        return a
    }
}

abstract class Data <: Base {
    public static func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo<!>() {
        return 1
    }
}

main(): Int64 {
    0
}
