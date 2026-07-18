

main(): Int64 {
    var a: Int64 = 0
    var res = match (a) {
        case b: Int64 => b
        case <!UNREACHABLE_PATTERN!>c: Int64<!> => c + 1
        case <!UNREACHABLE_PATTERN!>_<!> => -1
    }
    return res
}
