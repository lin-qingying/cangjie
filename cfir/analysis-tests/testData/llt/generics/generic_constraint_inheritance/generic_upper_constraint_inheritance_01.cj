

interface I {
    static func f<T1, T2>(): Unit where T1 <: Any {}
}

class A <: I {
    public static func f<T1, T2>() where T1 <: <!GENERIC_CONSTRAINT_NOT_LOOSER, UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T2<!> {}
}

main() {}
