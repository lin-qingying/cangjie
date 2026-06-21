


main() {
    var a = VArray<Int64, $<!LITERAL_NUMERIC_OVERFLOW!>18446744073709551616<!>>(repeat: 0)
    a[9223372036854775807 + 1]
    a[-9223372036854775808 - 1]

    var b = VArray<Int64, $9223372036854775807>(repeat: 0)
    <!BUILTIN_INDEX_IN_BOUND!>b<!>[9223372036854775807] // error

    var c = VArray<Int64, $1>(repeat: 0)
    c[9223372036854775807 + 1]
    c[-9223372036854775808 - 1]
    0
}
