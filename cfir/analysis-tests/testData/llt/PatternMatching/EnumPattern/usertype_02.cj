

enum E {
    | A(Int64)
    | A(Int64, Int64)
}

main() {
    let x = E.A(0)
    match (x) {
        case A(0) => 0
        case <!UNREACHABLE_PATTERN!>E.A(1)<!> => 1
        case <!UNREACHABLE_PATTERN!>A(_)<!> => 2
        case <!UNREACHABLE_PATTERN!>E.A(_, _)<!> => 3
    }
}
