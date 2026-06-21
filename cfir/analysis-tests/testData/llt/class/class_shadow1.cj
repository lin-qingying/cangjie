

open class Base {
    public let val = 1
}

class Data <: Base {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic static let val = 2
}

main(): Int64 {
    var res = Data.val
    return 0
}
