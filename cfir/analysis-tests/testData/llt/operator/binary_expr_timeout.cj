

main() {
    var a = "aaa"
    var b = 3 + 4 + 5 <!INVALID_BINARY_OPERATOR!>+<!> a + 5 + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 +
        a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a
    0
}
