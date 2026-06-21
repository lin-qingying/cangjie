

interface I<T> {}

class A<M, N> {}

extend A<String, String> <: <!EXTEND_DUPLICATE_INTERFACE!>I<String><!> {}

extend<T, D> A<T, D> <: I<D> {}

main() {}
