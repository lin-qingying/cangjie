

let t = while (true) {
    var l: Int32 = 0
    var r: Int64 = 1
    l <!INVALID_BINARY_OPERATOR!>+<!> r
}
