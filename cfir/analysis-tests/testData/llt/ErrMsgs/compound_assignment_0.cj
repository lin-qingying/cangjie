


main() {
    var b = 1 <!INVALID_BINARY_OPERATOR!>+<!> true
    var a = b

    a **= 1
    a *= true
    a /= true
    a %= true
    a += true
    a -= true
    a <<= true
    a >>= true
    a &&= true
    a ||= true
    a &= true
    a ^= true
    a |= true

    0
}
