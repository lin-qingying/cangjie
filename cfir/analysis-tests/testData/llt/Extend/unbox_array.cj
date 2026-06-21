

main() {
    var arr = [1, 2, 3]
    var c: Collection<Int64> = arr

    match (c) {
        case arr2: Array<Int64> =>
            arr2.clone();
            ()
        case _ => ()
    }
}
