


interface I<X> {}

open class C <: I<C> {}

class D <: C {}

func f<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Y<!>, Z>(a: Y) where Y <: Z, Z <: I<Y> {}

main() {
    f(D()) // see below
    0
}
