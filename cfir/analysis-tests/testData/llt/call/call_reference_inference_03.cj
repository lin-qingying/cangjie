

class A {
    func test<T>() {
        1
    }
    func test<T>(a: String) {
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
    let b = a.<!UNABLE_TO_INFER_GENERIC_FUNC!>test<!>
    return 0
}
