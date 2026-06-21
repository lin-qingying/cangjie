

main(): Int64 {
    let a: Float32 = 10.0
    let b: Bool = true

    if (a <!INVALID_BINARY_OPERATOR!>==<!> b) {
        return 1
    }
    return 0
}
