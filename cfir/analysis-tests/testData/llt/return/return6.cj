

func f(x!: Bool = <!INVALID_RETURN!>return true<!>, y!: Bool = x) {
    return y
}

main() {
    println(f())
}
