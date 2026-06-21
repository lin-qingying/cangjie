


main(): Int64 {
    var ((a, b), c): ((Int32, Int32), Int32) = ((0, 1), 2)
    var d = ((1, 2), (4, 5, 6))
    var e = d[0][0]
    var f = d[0][<!BUILTIN_INDEX_IN_BOUND!>2<!>]
    return 0
}
