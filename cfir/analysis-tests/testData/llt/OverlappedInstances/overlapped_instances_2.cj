


func f<X>(a: X) where X <: I<X> & I<C> {}

interface I<X> {}

open class C <: I<C> {}

open class D <: C & I<D> {}

main() {
    f(D())
    0
}
