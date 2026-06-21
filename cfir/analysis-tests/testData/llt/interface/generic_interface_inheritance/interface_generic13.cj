


class C {}

class D {}

interface I<T> {
    func foo(): T
}

interface II<T> <: I<C> & I<D> {
    func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(): C
}

main() {
    return 0
}
