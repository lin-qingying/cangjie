


class A {
    static const x = 0
    func f() {
        const t = x
        t
    }
}

main() {
    A().f()
}
