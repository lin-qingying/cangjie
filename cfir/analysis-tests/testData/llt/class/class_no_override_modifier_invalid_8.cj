


interface A<T> {
    func foo(a: T): T {
        return a
    }
}

interface B<T> {
    func foo(a: T): T {
        return a
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<T><!> <: A<T> & B<T> {}

main() {
    return 1
}
