

open class Base {
    public let val = 1
}

open class Data <: Base {
    public static let val2 = 2
}

class Derived <: Data {
    public static let <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>val<!> = 3
}

main(): Int64 {
    var res = Derived.val
    return 0
}
