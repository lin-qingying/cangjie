

enum E {
    | A(Bool, Int64)
    | B(Rune)
    | C
}

func f(x: E) {
    match (x) {
        case <!UNREACHABLE_PATTERN!>A(_: Int64, _: Int64)<!> => -1 // unreachable
        case A(_: Bool, _: Int64) => 0
        case B(_: ToString) => 1
        case C => 2
        case <!UNREACHABLE_PATTERN!>_<!> => -1 // unreachable
    }
}

main() {
    f(B(r'😂'))
}
