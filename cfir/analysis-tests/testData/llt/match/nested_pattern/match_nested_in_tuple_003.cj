


var r1 = match ((10, ())) {
    case <!UNREACHABLE_PATTERN!>(b: Int64, a: Int64)<!> => 1
    case <!UNREACHABLE_PATTERN!>(a: Unit, b: Int64)<!> => 1
    case <!UNREACHABLE_PATTERN!>(a: Unit, b: Unit)<!> => 1
    case (a: Int64, b: Unit) => 0
    case <!UNREACHABLE_PATTERN!>_<!> => 1
}

main(): Int64 {
    if (r1 != 0) {
        return 1
    }
    return 0
}
