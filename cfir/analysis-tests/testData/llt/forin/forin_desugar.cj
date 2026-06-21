


func f<T>() {
    var a: UInt8 = 0
    let s = "abc"
    for (i in s) {
        a++
    }
    return a
}

main() {
    if (f<Int64>() != 3) {
        return 1
    }
    return 0
}
