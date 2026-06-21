

enum MyList<T> {
    | Cons(T, MyList<T>)
    | Nil
}

main() {
    let x = MyList.Cons(0, Cons(1, Cons(2, Nil)))
    match (x) {
        case MyList.Nil => 0
        case MyList.Cons(_, Nil) => 1
        case _ => 2
    }
}
