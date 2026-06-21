

// Constraint check should pass
interface I {
    static func f<T1, T2, T3, T4>(): Unit where T1 <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T2<!>, T2 <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T3<!>, T3 <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T4<!> {}
}

class A <: I {
    public static func f<T1, T2, T3, T4>(): Unit where T1 <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T4<!> {}
}

main() {
}
