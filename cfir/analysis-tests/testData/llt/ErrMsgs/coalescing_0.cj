


main() {
    (Some(<!NO_MATCH_OPERATOR_FUNCTION_CALL!>1<!>(true)) ?? false) + true
    (Some(true) ?? <!CANNOT_CONVERT_LITERAL!>1<!>) * false

    let err = <!NO_MATCH_OPERATOR_FUNCTION_CALL!>1<!>(true)
    err ?? err
    0
}
