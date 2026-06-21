

main() {
    var a: Array<Option<Int64>> = [1, 2, 3]

    match (a[1]) {
        case Some(2) => println("OK")
        case _ => println("Error")
    }
}
