

func f(s: String) {
    1
}

main() {
    f(<!TYPE_MISMATCH!>(1, 2) == (1, 2)<!>)
}
