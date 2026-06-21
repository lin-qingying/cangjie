

main(): Int64 {
    var list0: Array<Array<Int64>> = [[1, 2, 3, 4], [5, 6, 7]]
    <!TYPE_MISMATCH!>list0[0]++<!>
    <!TYPE_MISMATCH!>list0[0]--<!>
    print("list0[0]: " + list0[0][0].toString())
    if (list0[0][0] == 1) {
        return 0
    }
    return 1
}
