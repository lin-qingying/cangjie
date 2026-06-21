

struct A<T> {
    A(var aa: T) {}
}

extend<V> A<V> {
    func foo() {
        return aa
    }
}

main() {
    return A<Int64>(-1).foo() + 1
}
