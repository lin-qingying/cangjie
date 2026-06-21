


class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>> where T <: Float32, V <: Float32 {
    func f(a: T) {
        return a <!INVALID_BINARY_OPERATOR!>+<!> a
    }
    func g(a: T) {
        return a <!INVALID_BINARY_OPERATOR!>+<!> 1.0
    }
    func g(a: T, b: V) {
        return a <!INVALID_BINARY_OPERATOR!>+<!> b
    }
    func g(a: T, b: Float32, c: V) {
        return 1.0 + a + 1.0 + b + 1.0 <!INVALID_BINARY_OPERATOR!>+<!> c + 1.0
    }
}

main() {
    var c = C<Float32, Float32>()
    if (c.f(1.0) < 1.999 || c.f(1.0) > 2.001) {
        return 1
    }
    if (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0) < 1.999 || c.g(1.0) > 2.001) {
        return 2
    }
    if (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0, 2.0) < 2.999 || c.g(1.0, 2.0) > 3.001) {
        return 3
    }
    if (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0, 2.0, 3.0) < 9.999 || c.g(1.0, 2.0, 3.0) > 10.001) {
        return 4
    }
    return 0
}
