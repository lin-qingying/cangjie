

main(): Int64 {
    var a: Int8 = 1
    var b: Int16 = 1
    var c: Int32 = 1
    var d: Int64 = 1
    var e: Int64 = 1
    var r1 = a <!INVALID_BINARY_OPERATOR!>&<!> b
    var r2 = a <!INVALID_BINARY_OPERATOR!>^<!> c
    var r3 = a <!INVALID_BINARY_OPERATOR!>|<!> e
    var r4 = a <!INVALID_BINARY_OPERATOR!>&<!> b ^ c << d | e
    var r5 = a <!INVALID_BINARY_OPERATOR!>|<!> e << c ^ d | b
    return 0
}
