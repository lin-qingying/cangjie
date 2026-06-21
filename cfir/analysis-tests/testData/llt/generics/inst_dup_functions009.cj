

interface I {
    func foo(a: Int32): Unit {}
}

extend<T> CPointer<T> <: I {
    func foo(a: T): Int32 {
        1
    }
}

main() {
    let x: CType = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>CPointer<Int32><!>()
}
