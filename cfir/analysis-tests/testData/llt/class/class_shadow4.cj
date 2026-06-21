

open class Base {
    public let val = 1
}

open class Data <: Base {
    public static let val2 = 2
}

class Derived <: Data {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic static let val = 3
}

main(): Int64 {
    var res = Derived.val
    return 0
}
