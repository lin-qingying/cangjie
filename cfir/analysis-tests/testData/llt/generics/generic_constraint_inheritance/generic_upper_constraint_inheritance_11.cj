

interface I {}

class A<T> where T <: I {}

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> A<T> where T <: String {}

main() {}
