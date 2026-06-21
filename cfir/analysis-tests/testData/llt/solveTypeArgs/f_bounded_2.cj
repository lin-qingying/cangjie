


interface I<X> {}

open class C <: I<C> {}

class D <: C {}

func f<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>X<!>, Y, Z>(a: X) where X <: Y, Y <: I<Y> & Z, Z <: X {}

main() {
    f(D())
    0
}
