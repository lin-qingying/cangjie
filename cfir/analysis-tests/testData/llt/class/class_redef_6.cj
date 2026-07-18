


open class C1 {
    public static func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public <!REDEF_INSTANCE_ERROR!>redef<!> func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo<!>(): Int64 {
        return 3
    }
}

main(): Int64 {
    return 1
}
