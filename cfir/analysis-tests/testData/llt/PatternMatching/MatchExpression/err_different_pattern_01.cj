

enum E {
    e
}

func f(x: Bool) {
    match (x) {
        case <!DIFFERENT_OR_PATTERN!>true | e<!> => 0
    }
}

main() {}
