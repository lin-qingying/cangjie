

func f(x!: Bool = if (true) {
        func g(x!: Bool = <!INVALID_RETURN!>return<!>) {}
        true
    } else {
        false
    }, y!: Bool = x) {
    return y
}

main() {
    println(f())
}
