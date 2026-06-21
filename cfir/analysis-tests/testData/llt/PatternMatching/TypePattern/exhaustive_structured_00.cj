

func f<T>(x: Option<T>) {
    match (x) {
        case Some(_) => 0
        case None => 1
        case <!UNREACHABLE_PATTERN!>_: ToString<!> => 2 // unreachable!
    }
}

main() {
    f(Some(0))
}
