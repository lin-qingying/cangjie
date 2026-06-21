

main() {
    let a = 1 <!INVALID_BINARY_OPERATOR!>+<!> true

    while (a) {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    }

    do {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    } while (a)

    for (x in [1] where a) {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    }

    for (a in a) {}

    0
}
