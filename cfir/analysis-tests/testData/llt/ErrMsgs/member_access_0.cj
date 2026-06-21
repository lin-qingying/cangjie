


main() {
    let a = 1 <!INVALID_BINARY_OPERATOR!>+<!> true

    (false <!INVALID_BINARY_OPERATOR!>+<!> 2).f()
    a.f()
    a.x

    0
}

// No errors:

// Errors:
