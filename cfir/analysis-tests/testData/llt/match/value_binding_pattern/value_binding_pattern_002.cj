

main():Int64 {
    let a = 10
    var b = match (a) {
        case <!UNREACHABLE_PATTERN!>11<!> => 1
        case c => 2
    }
    return 0
}
