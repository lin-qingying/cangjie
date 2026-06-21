


func f<X, Y>(a: X, b: Y) where X <: I<X> & I<C>, Y <: I<Y> & I<C> {}

interface I<X> {}

open class C <: I<C> {}

open class D <: C & I<D> {}

open class E <: D & I<E> {}

main() {
    f(E(), E())
    0
}
