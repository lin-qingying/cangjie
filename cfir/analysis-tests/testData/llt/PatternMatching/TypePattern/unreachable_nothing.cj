


func f(x: Int64) {
    match (x) {
        case <!UNREACHABLE_PATTERN!>_: Nothing<!> => -1 // unreachable
        case _: ToString => 0
    }
}

main() {
    f(return 0)
}
