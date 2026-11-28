extend<T> CPointer<T> {
    private func g(v: T) { let _ = v }
}

extend<T> CPointer<T> {
    public func g(v: Int64) { let _ = v }
}

main() {
    let _ = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>CPointer<Int64><!>()
}
