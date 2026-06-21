


main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false

    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>
    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>.e2
    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>.e2(e3)
    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>.e2<Bool>
    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>.e2<Bool>(e3)

    err.e2
    err.e2(e3)
    err.e2<Bool>
    err.e2<Bool>(e3)

    0
}

// Errors:

// No errors:
