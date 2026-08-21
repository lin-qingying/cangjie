main() {
    var x = 0
    while (x < 3) {
        for (a in [1] where break) {
            print(a)
        }
        x++
    }
    0
}