

enum Option<T> {
    Some(T)
}

main() {
    let x: Int64 = 17
    let y = x as Int64
    match (y) {
        case Some(_) => 0
        case _ => 1
    }
}
