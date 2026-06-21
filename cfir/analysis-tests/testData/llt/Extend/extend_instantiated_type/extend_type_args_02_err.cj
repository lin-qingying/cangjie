

class A<T> {}

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> A<T> where T <: String {
    func test() {
        2
    }
}

extend<T> A<T> {
    func foo() {
        return <!UNRESOLVED_REFERENCE!>test<!>()
    }
}

main() {
}
