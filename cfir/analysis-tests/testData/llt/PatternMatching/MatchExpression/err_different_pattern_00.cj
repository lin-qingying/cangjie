

func f(x: Bool) {
    match (x) {
        case true | <!VAR_IN_OR_PATTERN!>x<!> => 0
    }
}

main() {}
