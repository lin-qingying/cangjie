

main() {
    var a: Array<Any> = [1, "a"]

    for (x in a) {
        match (x) {
            case y: Int64 => println(y)
            case y: String => println(y)
            case _ => println("wildcard")
        }
    }
}
