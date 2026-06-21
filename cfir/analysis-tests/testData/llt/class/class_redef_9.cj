


open class C1 {
    public static func foo(a: Int64): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public static redef func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(a: Int64): Int32 {
        return 3
    }
}

main(): Int64 {
    return 1
}
