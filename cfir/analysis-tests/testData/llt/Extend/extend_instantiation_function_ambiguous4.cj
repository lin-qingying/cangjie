

enum e<T> {
    | A(Int32)
    | B(Int32, Int32)

    public func foo(a: Int32) {
        0
    }
}

extend<T> e<T> {
    public func foo(a: T) {
        1
    }
}

main() {
    let a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>e<Int32><!>.A(1)
    return 0
}
