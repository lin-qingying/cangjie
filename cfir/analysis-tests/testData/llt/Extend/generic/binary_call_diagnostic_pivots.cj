class Broken<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>V<!>> where T <: Float32, V <: Float32 {
    func g(a: T) { return a <!INVALID_BINARY_OPERATOR!>+<!> 1.0 }
    func g(a: T, b: V) { return a <!INVALID_BINARY_OPERATOR!>+<!> b }
    func g(a: T, b: Float32, c: V) { return 1.0 + a + 1.0 + b + 1.0 <!INVALID_BINARY_OPERATOR!>+<!> c + 1.0 }
}
func keep(a: Int64): Int64 { a }
main() {
    let c = Broken<Float32, Float32>()
    let leaves = c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0) + c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0)
    let nested = (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0) < 1.0) || (c.g(1.0) > 2.0)
    let rightOnly = (0 < 1) || (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0) > 2.0)
    let opaqueRight = (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0) < 1.0) || (keep(c.g(1.0)) > 2)
    let nameRight = (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0) < 1.0) || (<!UNRESOLVED_REFERENCE!>missing<!>(1.0) > 2)
    let receiverRight = (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0) < 1.0) || (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0).missing() > 2)
    let tree = (c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0) + c.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>(1.0)) + (c.g(1.0) + c.g(1.0))
    return 0
}
