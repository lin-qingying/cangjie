

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
}

main(): Int64 {
    var time = Year(1)
    var b = match (time) {
        case <!UNREACHABLE_PATTERN!>Month(1, 2)<!> | <!UNREACHABLE_PATTERN!>Year(1)<!> => 0
        case <!UNREACHABLE_PATTERN!>Year(1)<!> => 1
        case <!UNREACHABLE_PATTERN!>_<!> => -1
    }
    return b
}
