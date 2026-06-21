

func f(x: Array<Int8>) {
    x.size
}

func g(x: Int64) {
    x + 1
}

main() {
    (f ~> g)(1, 2, 3)
}
