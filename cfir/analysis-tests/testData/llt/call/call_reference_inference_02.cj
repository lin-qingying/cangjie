

class A {
    func test() {
        1
    }
    func test(a: String) {
        0
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
    let b = a.<!AMBIGUOUS_USE!>test<!>
    return 0
}
