

class A {
    func test() {
        1
    }
    private func test(a: Int64) {
        2
    }
    private func test(b: Rune) {
        3
    }
}

main() {
    let a = A()
    let b = a.test
    if (b() != 1) {
        return 1
    }
    return 0
}
