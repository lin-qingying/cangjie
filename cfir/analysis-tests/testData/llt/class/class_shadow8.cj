

open class Base {
    public static let val: Int32 = 0
    public static func f(): Int32 {
        return 0
    }
}

class Data <: Base {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic let val: Int32 = 1
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic let f: Int32 = 2
}

main(): Int64 {
    var data = Data()
    var d = data.val
    var f = data.f
    if (d != 1 || f != 2) {
        return 1
    }
    return 0
}
