func foo(): (Int64, Int64, Int64, Int64, Int64) {
    return (1, 2, 3, 4, 5)
}

main(): Int64 {
    var (a, b, c, d, e) = foo()
    println("after:${a},${b},${c},${d},${e}")
    return 0
}
