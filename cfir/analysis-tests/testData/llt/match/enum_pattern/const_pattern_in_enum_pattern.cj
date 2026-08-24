


enum TimeUnit1 {
    Year(String)
}

main(): Int64 {
    var str = Year("abc")
    var a = match (str) {
        case Year("x") => 1
        case <!UNREACHABLE_PATTERN!>Year("abc")<!> => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
    return a
}
