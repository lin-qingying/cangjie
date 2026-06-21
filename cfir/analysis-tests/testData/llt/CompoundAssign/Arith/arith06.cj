


main(): Int64 {
    var a: Float32 = 1.0
    a += 2.0
    a += 3.14
    var b = a + 1.0
    if (b - 7.14 < 0.0001) {
        return 0
    }
    return 1
}
