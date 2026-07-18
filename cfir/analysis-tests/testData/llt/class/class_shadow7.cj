

open class Base {
    public let f = 0
}

class Data <: Base {
    public static func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>f<!>(): Int32 {
        return 0
    }
}

main(): Int64 {
    var res = Data.f()
    return 0
}
