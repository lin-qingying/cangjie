enum E { A }

func matchKinds(value: Bool): Unit {
    match (value) {
        case <!DIFFERENT_OR_PATTERN!>true | E.A | (_, _)<!> => ()
    }
}

func tupleKinds(value: (Bool, Bool)): Unit {
    match (value) {
        case <!DIFFERENT_OR_PATTERN!>(true, _) | true<!> => ()
    }
}

func ifKinds(value: Bool): Unit {
    if (let <!DIFFERENT_OR_PATTERN!>true | E.A | (_, _)<!> <- value) { print(value) }
}

func whileKinds(value: Bool): Unit {
    while (let <!DIFFERENT_OR_PATTERN!>true | E.A | (_, _)<!> <- value) { break }
}
