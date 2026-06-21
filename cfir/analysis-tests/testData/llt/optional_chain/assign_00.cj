


main() {
    var a = Some([0, 1])
    a?[0] = 1
    match (a?[0]) {
        case Some(t) => t - 1
        case None => 1
    }
}
