


class C {}

class D {}

class E {}

interface I<T> {
    func foo(): T
}

interface II<T> <: I<C> & I<D> & I<E> {
    func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(): C
}

main() {
    return 0
}
