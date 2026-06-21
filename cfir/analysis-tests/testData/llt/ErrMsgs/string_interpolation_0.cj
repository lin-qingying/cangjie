

main() {
    let err1 = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    "a${err1}c"
    let b: String = "a${err1}c"

    let err2 = "a${err1}c"

    err2 + 1
    err2 * false
    err2(true)
    0
}
