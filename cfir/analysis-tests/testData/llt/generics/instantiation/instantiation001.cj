

main(): Int64 {
    if (foo2().get(2) != 1) {
        print("${foo2().get(2)}")
        return 1
    }
    return 0
}

func foo2(): List<Int64> {
    var a = List<Int64>([1, 2, 3])
    return a
}
