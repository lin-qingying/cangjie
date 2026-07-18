

open class Base {
    public let val = 1
}

class Data <: Base {
    public static let <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>val<!> = 2
}

main(): Int64 {
    var res = Data.val
    return 0
}
