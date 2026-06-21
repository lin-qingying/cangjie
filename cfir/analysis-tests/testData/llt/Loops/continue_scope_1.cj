


main() {
    var iter = 5
    while (iter > 0) {
        iter--
        do {
            println("inner")
        } while (continue)
        println("outer")
    }
    0
}
