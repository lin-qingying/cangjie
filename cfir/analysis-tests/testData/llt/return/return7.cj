

func f(x!: Bool = if (true) {
        <!INVALID_RETURN!>return true<!>
    } else {
        false
    }, y!: Bool = x) {
    return y
}

main() {
    println(f())
}
