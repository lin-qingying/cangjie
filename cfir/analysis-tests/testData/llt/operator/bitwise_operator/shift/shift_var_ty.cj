

main(): Int64 {
    var varRune: Rune = '1'
    varRune = varRune <!INVALID_BINARY_OPERATOR!><<<!> 1
    return 0
}
