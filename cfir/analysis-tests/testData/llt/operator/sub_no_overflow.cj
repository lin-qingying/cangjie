

func run(): Int64 {
    var bu: Int8 = -128 - (-1)
    var cu: Int16 = -32768 - (-1)
    var eu: Int32 = -2147483648 - (-1)
    var fu: Int64 = -9223372036854775808 - (-1)
    var bu1: Int16 = 12 - (-12)
    bu1 = 1 - (-1)

    return 0
}

main(): Int64 {
    return run();
}
