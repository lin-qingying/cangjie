

main(): Int64 {
    var i: Int64 = 1

    var a: Array<Array<Int64>> = Array<Array<Int64>>(i, Array<Int64>(i, <!ARGUMENT_TYPE_MISMATCH!>i<!>))

    return 0
}
