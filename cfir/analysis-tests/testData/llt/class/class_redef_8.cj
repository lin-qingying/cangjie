


open class C1 {
    public static func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public static <!NOTHING_TO_OVERRIDE!>redef<!> func foo(a: Int64): Int64 {
        return 3
    }
}

main(): Int64 {
    return 1
}
