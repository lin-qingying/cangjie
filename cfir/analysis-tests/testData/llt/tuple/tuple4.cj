


main(): Int64 {
    var (a, b, c) = (0, 1, 2)
    var d = (3, 4, (a, b, c)[<!BUILTIN_INDEX_IN_BOUND!>3<!>])
    var (one, two, three): (Int32, Int32, Int32) = <!TYPE_MISMATCH!>(5, 6, 7, 8)<!>
    return 0
}
