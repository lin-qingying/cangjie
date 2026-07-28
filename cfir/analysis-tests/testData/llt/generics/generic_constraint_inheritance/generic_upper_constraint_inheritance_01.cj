

interface I {
    static func f<T1, T2>(): Unit where T1 <: Any {}
}

class A <: I {
    public static func f<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T1<!>, T2>() where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T1 <: T2<!> {}
}

main() {}
