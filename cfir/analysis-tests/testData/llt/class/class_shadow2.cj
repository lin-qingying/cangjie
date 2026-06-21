

open class Base {
    public func f() {
        return 1
    }
}

class Data <: Base {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic static let f = 2
}

main(): Int64 {
    var res = Data.f
    return 0
}
