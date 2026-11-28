extend<T> CPointer<T> {
    private func g(v: T) { let _ = v }
}

extend<T> CPointer<T> {
    private func g(v: Int64) { let _ = v }
}

main() {
    let _ = CPointer<Int64>()
}
