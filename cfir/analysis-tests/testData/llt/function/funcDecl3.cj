

func f(a: Unit) {}

func f() {}

func f0(a: Int32) {
    var b = a
    a + b
}

func f1(a: Int32) {
    a + 2
}

main() {
    if (f0(3) == 6 && f1(2) == 4) {
        return 0
    }
    return 1
}
