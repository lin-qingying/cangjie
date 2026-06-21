

class A<T> {}

class B<T> {}

class C<T> {}

extend<T> A<C<T>> {
    func test() {
        22
    }
}

main() {
    let a1 = A<B<Int64>>()
    a1.<!NOT_MEMBER_OF!>test<!>()
    return 0
}
