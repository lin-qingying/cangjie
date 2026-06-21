


main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>&&<!> false
    err ~> err
    0
}
