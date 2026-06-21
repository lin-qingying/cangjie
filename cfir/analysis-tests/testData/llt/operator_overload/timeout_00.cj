

enum E {
    A
}

main() {
    let x = A + A <!INVALID_BINARY_OPERATOR!>*<!> A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A +
        A * A
}
