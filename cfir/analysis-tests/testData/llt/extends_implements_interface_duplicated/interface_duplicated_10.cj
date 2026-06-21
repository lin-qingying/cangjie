


interface I<K> {}

open class A1<T> <: B<Int64> & I<Int64> {}

open class A2<T> <: B<T> & I<Int64> {}

open class B<T> <: I<T> {}

open class C<L> <: A2<L> & I<Int32> & I<Rune> {}

open class D<P> <: C<P> {}

open class E<U> <: D<Int32> {}

main() {
    return 0
}
