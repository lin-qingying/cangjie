

func f(x: Int64) {
    match (x) {
        case <!UNREACHABLE_PATTERN!>_: Bool<!> => -1 // unreachable
        case _ => 0
        case <!UNREACHABLE_PATTERN!>_: ToString<!> => -1 // unreachable
    }
}

main() {
    f(0)
}
