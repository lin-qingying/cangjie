

open class Base {
    public func f() {
        return 1
    }
}

class Data <: Base {
    public static let <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>f<!> = 2
}

main(): Int64 {
    var res = Data.f
    return 0
}
