

enum MyList<T> {
    | Cons(T, MyList<T>)
    | Nil
}

main() {
    let x = Cons(0, Cons(1, Cons(2, Nil)))
    match (x) {
        case MyList<Int64>.Nil => 0
        case Cons(_, Nil) => 1
        case Cons(_, MyList<Int64>.Cons(_, Nil)) => 2
        case MyList<Int64>.Cons(_, Cons(_, MyList<Int64>.Cons(_, MyList<Int64>.Nil))) => 3
        case _ => 4
    }
}
