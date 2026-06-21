


main() {
    var iter = 5
    while (iter > 0) {
        println("outer")
        do {
            println("inner")
        } while (break)
        iter--
    }
    0
}
