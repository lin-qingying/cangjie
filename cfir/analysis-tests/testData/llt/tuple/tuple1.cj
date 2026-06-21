

main(): Int64 {
    var a: (Int64, Int64, Int64, Int64) = (0, 1, 2, 3)
    var b = (5, 6, 7)
    var c: (Int64, Int64, Int64) = b
    var d = (8, a[0], 9)
    var e: (Int64, (Int64, Int64)) = (11, (22, 33))
    return a[0]
}
