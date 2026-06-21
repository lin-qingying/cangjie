


main(): Int64 {
    var sum = 0
    var n: Int8 = 127
    for (i in 0..=n) {
        println(i)
        sum += Int64(i)
    }
    if (sum != 8128) {
        return 1
    }
    return 0
}
