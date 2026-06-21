

main(): Int64 {
    var numOther = <!LITERAL_NUMERIC_OVERFLOW!>18446744073709551615<!>
    println(numOther is Int64)
    println(numOther)
    return 0
}
