

func run(): Int64 {
    var p6: Int64 = 2 ** 62
    p6 = 2 ** 62 - 1 + 2 ** 62
    var p7: Int64 = -2 ** 63

    if (p6 != 9223372036854775807) {
        return 1
    }

    return 0
}

main(): Int64 {
    return run();
}
