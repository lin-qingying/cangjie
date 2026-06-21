

func f(x!: Bool = if (true) {
        func g(x!: Bool = <!INVALID_RETURN!>return<!>) {
            return
        };
        <!INVALID_RETURN!>return true<!>
    } else {
        false
    }, y!: Bool = x) {
    return y
}

main() {
    let x: Bool = f()
}
