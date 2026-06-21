

main() {
    var err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false

    !err
    -err
        + err
    err++
    err--

    0
}
