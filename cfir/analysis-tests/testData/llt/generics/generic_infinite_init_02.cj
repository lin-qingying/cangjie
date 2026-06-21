


class A<T> {
    func test(a: <!GENERIC_INFINITE_INSTANTIATION!>B<!><A<T>>) {}
}

class B<T> {
    func test(a: A<(T, T)>) {}
}

main() {
    A<Int32>()
    0
}
