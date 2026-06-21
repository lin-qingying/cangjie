

main() {
    var a = 1 <!INVALID_BINARY_OPERATOR!>+<!> true
    1 <!INVALID_BINARY_OPERATOR!>+<!> true + a

    1 + 2 + a
    a - 2 - 3
    a ** 1
    a * true
    a / true
    a % true
    a + true
    a - true
    a << true
    a >> true
    a && true
    a || true
    a & true
    a ^ true
    a | true

    0
}
