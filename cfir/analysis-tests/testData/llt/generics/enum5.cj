


enum TimeUnit<T> {
    Year(T) | Month(T)
}

var time = <!INVALID_TYPE_PARAM_OF_ENUM_MEMBER_ACCESS!>T<!>imeUnit<Int32>.Year<Int64>(4)
var res1: Int32 = match (time) {
    case Year(x) => 2020
    case Month(x) => 9
}

func run(): Int64 {
    if (res1 == 2020) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}
