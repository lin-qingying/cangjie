

enum Color {
    R | G | B(Int64)
}

func foo(a: (?Int64, Color)) {
    if (let (Some(a), B(b)) <- a) {
        a + b
    } else {
        -1
    }
}

main() {
    println(foo((Some(3), B(15))))
    println(foo((Some(3), R)))
    println(foo((None, B(15))))
    println(foo((None, R)))
}
