

interface I1 {
    static func foo(): Int64 {
        return 1
    }
}

class C1 <: I1 {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 1
}
