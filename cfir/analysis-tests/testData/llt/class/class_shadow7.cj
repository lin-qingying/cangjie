

open class Base {
    public let f = 0
}

class Data <: Base {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic static func f(): Int32 {
        return 0
    }
}

main(): Int64 {
    var res = Data.f()
    return 0
}
