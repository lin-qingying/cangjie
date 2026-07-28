

interface I<T> {
    static func f<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T1<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T2<!>>(): Unit where T1 <: T2, T2 <: T {}
}

class A <: I<Int64> {
    public static func f<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T1<!>, T2>(): Unit where T1 <: Int64 {}
}

main() {}
