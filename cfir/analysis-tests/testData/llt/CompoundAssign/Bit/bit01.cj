


main(): Int64 {
    var a = 256
    var b = 16
    a >>= 3
    b <<= 2
    if (a == 32 && b == 64) {
        return 0
    }
    return 1
}
