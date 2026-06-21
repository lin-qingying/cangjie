

enum TimeUnit1 {
    Hour
    | Year
}

enum TimeUnit2 {
    Month
}

func run(): Int64 {
    var time = TimeUnit2.Month
    var res: Int32 = match (time) {
        case <!ENUM_PATTERN_PARAM_SIZE_ERROR!>Month(Hour)<!> => 2020
    }
    return 0
}
