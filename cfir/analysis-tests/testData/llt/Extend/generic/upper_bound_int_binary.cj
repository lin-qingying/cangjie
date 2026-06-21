


class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>> where T <: Int8, V <: Int8 {
    func f(a: T) {
        return a <!INVALID_BINARY_OPERATOR!>+<!> a
    }
    func g(a: T) {
        return a <!INVALID_BINARY_OPERATOR!>+<!> 1
    }
    func g(a: T, b: V) {
        return a <!INVALID_BINARY_OPERATOR!>+<!> b
    }
    func g(a: T, b: Int8, c: V) {
        return 1 + a + 1 + b + 1 <!INVALID_BINARY_OPERATOR!>+<!> c + 1
    }
}

main() {
    var c = C<Int8, Int8>()
    if (c.f(1) != 2) {
        return 1
    }
    if (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1) != 2) {
        return 2
    }
    if (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1, 2) != 3) {
        return 3
    }
    if (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1, 2, 3) != 10) {
        return 4
    }
    return 0
}
