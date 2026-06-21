


open class A {}

class E<X, Y> {
    public static func f(a: X, b: Y) {
        print("f(a: X, b: Y)")
    }
    public static func f(a: A, b: Int64) {
        print("f(a: X, b: Int64)")
    }
}

main() {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>E<A, Int64><!>.<!AMBIGUOUS_FUNCTION_CALL!>f<!>(A(), 1)
    return 0;
}
