


type A<T, U> = C<U, Base<T>>

class C2 {}

class Base<Z> where Z <: C2 {}

class C<X, Y> where X <: Base<Y> {}

main() {
    return 0;
}
