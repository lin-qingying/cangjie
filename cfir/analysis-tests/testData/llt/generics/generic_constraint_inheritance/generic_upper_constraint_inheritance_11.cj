

interface I {}

class A<T> where T <: I {}

extend<T> A<T> where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>String<!> {}

main() {}
