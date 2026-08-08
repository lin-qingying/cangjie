


main(): Int64 {
    return case01()
}

func case01() {
    let a = 0..10 : 1
    var result = match (0..10 : 1) {
        case a => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }
    return result
}
