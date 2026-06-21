


class A {}

main() {
    let f = {
        =>
        if (true) {
            return 1
        }
        return A()
    }
    if (f() is Any) {
        return 0
    }
    return 1
}
