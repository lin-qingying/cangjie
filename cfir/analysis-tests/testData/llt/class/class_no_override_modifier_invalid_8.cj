


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

class <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>D<!><T> <: A<T> & B<T> {}

main() {
    return 1
}
