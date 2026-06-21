

interface I<T> {
    static func f<T1, T2>(): Unit where T1 <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T2<!>, T2 <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!> {}
}

class A <: I<Int64> {
    public static func f<T1, T2>(): Unit where T1 <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Int64<!> {}
}

main() {}
