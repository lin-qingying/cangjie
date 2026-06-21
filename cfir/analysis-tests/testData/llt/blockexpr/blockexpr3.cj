

main(): Int64 {
    var a: Array<Int32> = [1, 2]
    if (a[{=> 0}()] != 1 || a[1] != 2) {
        return 1
    }
    return 0
}
