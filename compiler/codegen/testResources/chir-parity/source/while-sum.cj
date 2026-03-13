func sumTo(n: Int64): Int64 {
    var i = 0
    var acc = 0
    while (i <= n) {
        acc = acc + i
        i = i + 1
    }
    return acc
}

main(): Int64 {
    return sumTo(10)
}
