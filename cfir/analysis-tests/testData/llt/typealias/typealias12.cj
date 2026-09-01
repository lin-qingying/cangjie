


type Opt<T> = Option<T>

main(): Int64 {
    var time: Opt<Int32> = Option<Int32>.Some(2020)
    var res: Int32 = match (time) {
        case Some(x) => x
        case <!UNREACHABLE_PATTERN!>None<!> => 11
    }
    if (res == 2020) {
        return 0
    }
    return 1
}
