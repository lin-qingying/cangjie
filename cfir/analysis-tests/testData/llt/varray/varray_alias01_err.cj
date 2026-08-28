


type varr = VArray<Rune, $3>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type varr1_1<T><!> = VArray<Int64, $3>

// Omit Generic Arguments.

main() {
    var a: varr = varr(repeat: <!CANNOT_CONVERT_LITERAL!>0<!>)

    var c: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>varr1_1<!> = varr1_1(repeat: 0) // error x2
}
