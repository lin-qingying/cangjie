

interface I<T> {}

class A<K> {}

extend A<(String, String)> <: <!EXTEND_DUPLICATE_INTERFACE!>I<String><!> {} // error

extend<T> A<(T, T)> <: I<T> {} // error

main() {}
