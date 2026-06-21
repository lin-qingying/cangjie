

interface I<T> {}

class A<M, N> {}

extend<T> A<T, String> <: <!EXTEND_DUPLICATE_INTERFACE!>I<String><!> {}

extend<Q, R> A<Q, R> <: I<R> {}

main() {}
