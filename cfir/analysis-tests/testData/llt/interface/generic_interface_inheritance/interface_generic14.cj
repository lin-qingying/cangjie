


class C {}

class D {}

class E {}

interface I<T> {
    func foo(): T
}

interface II<T> <: I<C> & I<D> & I<E> {
    func <!RETURN_TYPE_INCOMPATIBLE!>foo<!>(): C
}

main() {
    return 0
}
