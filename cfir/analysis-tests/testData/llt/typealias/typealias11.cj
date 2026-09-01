


enum TimeUnit<T> {
    Year(T) | Month
}

type Time<T> = TimeUnit<T>

main(): Int64 {
    var time: Time<Int32> = TimeUnit<Int32>.Year(2020)
    var res: Int32 = match (time) {
        case Year(x) => x
        case <!UNREACHABLE_PATTERN!>Month<!> => 11
    }
    if (res == 2020) {
        return 0
    }
    return 1
}
