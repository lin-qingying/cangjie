

interface I<T> {}

class A<K> {}

extend A<String> <: <!EXTEND_DUPLICATE_INTERFACE!>I<String><!> {} // error

extend<T> A<T> <: I<T> {} // error

main() {}
