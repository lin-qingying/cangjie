

func test(): Int64 {
    var i: Int32 = 0
    var res = match (i + 1) {
        case <!UNREACHABLE_PATTERN!>0<!> => 1
        case 1 => 0
        case <!UNREACHABLE_PATTERN!>_<!> => -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
