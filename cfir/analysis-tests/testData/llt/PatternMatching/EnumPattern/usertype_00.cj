


package myenum

enum E {
    | A
    | B
}

main() {
    let x = A
    match (x) {
        case E.A => 0
        case <!UNREACHABLE_PATTERN!>A<!> => 0
        case <!UNREACHABLE_PATTERN!>E.A<!> => 0
        case B => 1
        case <!UNREACHABLE_PATTERN!>E.B<!> => 1
        case <!UNREACHABLE_PATTERN!>E.B<!> => 1
    }
}
