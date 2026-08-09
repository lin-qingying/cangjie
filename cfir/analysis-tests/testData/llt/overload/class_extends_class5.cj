

main(): Int64 {
    return 0
}

abstract class Base {
    public static func foo(): Int32 {
        return 0
    }
}

class Data <: Base {
    public func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo<!>(): Int32 {
        return 4
    }
}
