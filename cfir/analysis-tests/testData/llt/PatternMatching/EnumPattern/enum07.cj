

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d = match (time) {
        case <!UNREACHABLE_PATTERN!>Year(a)<!> => a
        case <!UNREACHABLE_PATTERN!>Month(a, b)<!> => a + b
        case Day(a, b, c) =>
            print(a.toString() + "," + b.toString() + "," + c.toString())
            a + b + c
        case <!UNREACHABLE_PATTERN!>Hour(1, 2, 3, 4)<!> => Int32(-1)
        case <!UNREACHABLE_PATTERN!>Hour(a, b, c, d)<!> => a + b + c + d
    }
    if (d == 6) {
        return 0
    }
    return 1
}
