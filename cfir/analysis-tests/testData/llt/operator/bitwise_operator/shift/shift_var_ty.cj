

main(): Int64 {
    var varRune: Rune = '1'
    varRune = <!INVALID_BINARY_OPERATOR!>varRune << 1<!>
    return 0
}
