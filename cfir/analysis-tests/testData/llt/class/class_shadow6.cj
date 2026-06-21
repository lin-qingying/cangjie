


open class Base {
    public func f(): Int32 {
        return 1
    }
}

class Data <: Base {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic static func <!CANNOT_OVERRIDE!>f<!>(): Int32 {
        return 0
    }
}

main(): Int64 {
    var res = Data.f()
    return 0
}
