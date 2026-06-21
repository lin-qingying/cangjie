


main(): Int64 {
    var varr: VArray<Int64, $<!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>> = VArray<Int64, $<!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>> {i => i + 1}
    return varr[0] - 1
}
