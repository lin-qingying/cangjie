

func f() {
    var b: Int32
    do {
        if (true) {
            for (i in 0..2) {
                continue
            }
        }
        while (true) {
            break
        }
        b = 1
    } while (b == 0)
    b
}

main() {
    return f() - 1
}
