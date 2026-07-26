


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

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>D<!><T> <: A<T> & B<T> {}

main() {
    return 1
}
