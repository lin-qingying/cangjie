

interface I1<T> {
    func foo() {
        1
    }
}

interface I2<U> <: I1<U> {
    func foo() {
        2
    }
}

class A<V> {}

extend<W> A<W> <: I2<W> {}

extend<X> A<X> <: I1<X> {}

class B<S> {}
// Diff order.
extend<Y> B<Y> <: I1<Y> {}

extend<Z> B<Z> <: I2<Z> {}

main() {
    println(A<Int32>().foo())
    println(B<Int32>().foo())
}
