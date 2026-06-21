

func test(): Int64 {
    var a = 1
    var b = 2
    var res = match {
        case a > b =>
            var x = 0
            x + 1
        case a < b =>
            var x = a + b - 3
            x
        case _ => -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
