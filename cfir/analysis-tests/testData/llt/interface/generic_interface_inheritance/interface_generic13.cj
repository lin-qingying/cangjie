


class C {}

class D {}

interface I<T> {
    func foo(): T
}

interface II<T> <: I<C> & I<D> {
    func <!RETURN_TYPE_INCOMPATIBLE!>foo<!>(): C
}

main() {
    return 0
}
