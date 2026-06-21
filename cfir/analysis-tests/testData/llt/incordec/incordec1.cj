


main(): Int64 {
    var tuple = (1, 2, 3)
    <!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>tuple[0]<!>++
    return tuple[0]
}
