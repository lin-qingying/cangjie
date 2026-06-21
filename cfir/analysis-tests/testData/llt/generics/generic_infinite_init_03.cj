


// Check for different decl order with 'generic_infinite_init_02.cj'

main() {
    A<Int32>()
    0
}

class A<T> {
    func test(a: <!GENERIC_INFINITE_INSTANTIATION!>B<!><A<T>>) {}
}

class B<T> {
    func test(a: A<(T, T)>) {}
}
