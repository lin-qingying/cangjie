


class C<X, Y> {
    public static func f(a: X, b: Y) {}
    public static func f(a: Bool, b: Int64) {}
}

interface I<X, Y> {
    static func f(a: X, b: Y) {}
    static func f(a: Bool, b: Int64) {}
}

struct R<X, Y> {
    public static func f(a: X, b: Y) {}
    public static func f(a: Bool, b: Int64) {}
}

main() {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Bool, Int64><!>.<!AMBIGUOUS_FUNCTION_CALL!>f<!>(true, 1)
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I<Bool, Int64><!>.<!AMBIGUOUS_FUNCTION_CALL!>f<!>(true, 1)
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>R<Bool, Int64><!>.<!AMBIGUOUS_FUNCTION_CALL!>f<!>(true, 1)
    return 0;
}
