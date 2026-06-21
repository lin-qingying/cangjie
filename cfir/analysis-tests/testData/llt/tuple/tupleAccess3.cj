

main(): Int64 {
    var tup: (Int64, Int64) = (444, 888)
    var removedOpt: Option<Int64> = tup[1]
    if ((removedOpt ?? -1) != 888) {
        return 1
    }
    return 0
}
