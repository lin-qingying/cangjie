


main() {
    let a = Some([0, 1])
    let b = 0
    let x = a?[b]
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
