

func test(): Int64 {
    var i: Int32 = 0
    var x: Bool = false
    var y: Bool = true
    var flag: Bool = true
    var res = match (i + 1) {
        case 0 => 1
        case 1 where if (flag) {
            x
        } else {
            y
        } => 0
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
