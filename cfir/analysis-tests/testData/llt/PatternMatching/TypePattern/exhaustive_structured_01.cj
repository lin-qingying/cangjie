

func f<T>(x: Option<T>) {
    match (x) {
        case _: ToString => 0 // maybe reachable!
        case Some(_) => 1
        case None => 2
    }
}

main() {
    f(Some(0))
}
