

func test(): Int64 {
    var i: Int32 = 0
    var flag: Bool = true
    var res = match (i++) {
        case _ => flag
    }
    if (res) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
