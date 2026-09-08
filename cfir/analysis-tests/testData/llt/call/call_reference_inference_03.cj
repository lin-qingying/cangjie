

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
    let b = a.<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>test<!>
    return 0
}
