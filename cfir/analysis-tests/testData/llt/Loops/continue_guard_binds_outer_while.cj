main() {
    var x = 0
    while (x < 3) {
        for (a in [1] where continue) {
            print(a)
        }
        x++
    }
    0
}