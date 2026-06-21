


interface A<X, Y> where X <: A<X, X> & B<Y, Y>, Y <: A<Y, Y> & B<X, X> {}

interface B<X, Y> where X <: A<X, X> & B<Y, Y>, Y <: A<Y, Y> & B<X, X> {}

class C <: A<C, C> & B<C, C> {}

main(): Int64 {
    return 0
}
