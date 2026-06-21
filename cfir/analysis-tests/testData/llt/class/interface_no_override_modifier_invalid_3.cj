

interface I1 {
    static func foo(): Int64 {
        return 1
    }
}

interface I2 <: I1 {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>f<!>unc foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 1
}
