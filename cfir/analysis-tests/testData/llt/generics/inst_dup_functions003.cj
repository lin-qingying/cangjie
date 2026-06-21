


class C<T, U> {
    public static func foo(a: T, b: U) {}
    public static func foo(a: U, b: U) {}
}

struct R<T, U> {
    public static func foo(a: T, b: U) {}
    public static func foo(a: U, b: U) {}
}

func bar<X>(a: X) {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<X, X><!>.<!AMBIGUOUS_FUNCTION_CALL!>foo<!>(a, a)
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>R<X, X><!>.<!AMBIGUOUS_FUNCTION_CALL!>foo<!>(a, a)
}

main() {
    return 0;
}
