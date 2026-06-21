


main(): Int64 {
    var set1 = [1, 2, 3]
    set1[0] = set1[0] + 1
    set1[0] = set1[0] - 1
    if (set1[0] == 1) {
        return 0
    }
    return 1
}
