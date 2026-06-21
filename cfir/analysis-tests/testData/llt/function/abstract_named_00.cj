

interface I {
    static func f(a!: Int32): Int32 {
        a
    }
}

class A <: I {
    public static func <!PARAM_NAMED_MISMATCHED!>f<!>(a: Int32): Int32 {
        a + 1
    }
}

main() {
    A.f(0)
}
