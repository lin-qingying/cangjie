


main() {
    let a: Bool = true

    if (a) {
        true <!INVALID_BINARY_OPERATOR!>+<!> 0
    } else {
        false <!INVALID_BINARY_OPERATOR!>+<!> 1
    }

    let b: Bool = if (a) {
        true <!INVALID_BINARY_OPERATOR!>+<!> 0
    } else {
        false <!INVALID_BINARY_OPERATOR!>+<!> 1
    }

    if (a) {
        true <!INVALID_BINARY_OPERATOR!>+<!> 0
    }
    let c: Bool = if (a) {
        true <!INVALID_BINARY_OPERATOR!>+<!> 0
    }
    0
}
