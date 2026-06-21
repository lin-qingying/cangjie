


main(): Int64 {
    var sum = 0
    for (i in 127..0 : -1) {
        sum += 1
    }
    if (sum != 127) {
        return 1
    }
    return 0
}
