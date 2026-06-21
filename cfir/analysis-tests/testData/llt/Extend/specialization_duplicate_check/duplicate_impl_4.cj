

interface I<T> {}

class A<T> <: I<T> {}

extend A<String> <: <!EXTEND_DUPLICATE_INTERFACE!>I<String><!> {}

main() {}
