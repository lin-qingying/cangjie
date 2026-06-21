

main(): Int64 {
    var var1: (Int32, Int8) = (1, 2)
    if (((1, 2), 3)[0] != var1) {
        return 1
    }
    if (!(((1, 2), 3)[0] == var1)) {
        return 2
    }
    return 0
}
