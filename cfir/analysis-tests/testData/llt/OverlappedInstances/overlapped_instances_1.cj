


func f<X, Y>(a: I<X>, b: Y) {}

interface I<X> {}

class C<X> <: I<X> {}

extend<X> C<X> <: I<D> {}

open class D {}

class E <: D {}

main() {
    f(C<E>(), false)
    0
}
