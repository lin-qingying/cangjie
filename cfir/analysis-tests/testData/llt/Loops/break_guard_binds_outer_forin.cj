main() {
    for (b in [2]) {
        for (a in [1] where break) {
            print(a)
        }
        print(b)
    }
    0
}