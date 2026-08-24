

enum MyList<T> {
    | Cons(T, MyList<T>)
    | Nil
}

main() {
    let x = Cons(0, Cons(1, Cons(2, Nil)))
    match (x) {
        case <!UNREACHABLE_PATTERN!>MyList<Int64>.Nil<!> => 0
        case Cons(_, Nil) => 1
        case <!UNREACHABLE_PATTERN!>Cons(_, MyList<Int64>.Cons(_, Nil))<!> => 2
        case <!UNREACHABLE_PATTERN!>MyList<Int64>.Cons(_, Cons(_, MyList<Int64>.Cons(_, MyList<Int64>.Nil)))<!> => 3
        case <!UNREACHABLE_PATTERN!>_<!> => 4
    }
}
