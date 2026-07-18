

interface I1 {
    func foo(): Int64 {
        return 1
    }
}

class C1 <: I1 {
    public static func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo<!>(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 1
}
