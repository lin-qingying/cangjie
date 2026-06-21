


main() {
    var err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false

    match (err) {
        case x => 1 + false
        case y => 1 + true
        case z => err
        case phi => phi
    }

    0
}

// Errors:

// No errors:
