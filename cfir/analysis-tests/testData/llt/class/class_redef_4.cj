


open class C1 {
    public func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic static func <!CANNOT_OVERRIDE!>foo<!>(): Int64 {
        return 3
    }
}

main(): Int64 {
    return 1
}
