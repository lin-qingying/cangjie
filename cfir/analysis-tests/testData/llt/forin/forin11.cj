


main(): Int64 {
    var sum = 0
    for (i in 127..0 : -2) {
        sum += 1
    }
    if (sum != 64) {
        return 1
    }
    return 0
}
