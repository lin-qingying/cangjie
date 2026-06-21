


interface I<T> {}

class A<K> {}

class B<R> {}

extend Array<A<Int64>> <: I<Int64> {}

extend<T> Array<B<T>> <: I<T> {}

main() {}
