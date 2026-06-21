


main() {
    let err = <!NO_MATCH_OPERATOR_FUNCTION_CALL!>1<!>(false)
    spawn {err} && false
    spawn {err}.x

    spawn {err}
    0
}
