

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
}

main(): Int64 {
    var time = Year(1)
    var b = match (time) {
        case <!UNREACHABLE_PATTERN!>Month(1, 2)<!> => 1
        case <!UNREACHABLE_PATTERN!>Year(c)<!> =>
            print(c.toString())
            0
        case <!UNREACHABLE_PATTERN!>_<!> => -1
    }
    return b
}
