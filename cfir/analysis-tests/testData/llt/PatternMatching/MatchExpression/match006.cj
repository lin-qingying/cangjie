

func test(): Int64 {
    var i = 0
    let a = 1
    let b = 2
    var res = match (i) {
        case 1 | -2 | 0 where false => 1
        case 2 => 0
        case _ => -1
    }
    if (res == -1) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
