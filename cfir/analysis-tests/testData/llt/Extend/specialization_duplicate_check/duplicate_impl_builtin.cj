

interface I<T> {}

extend CPointer<Int64> <: <!EXTEND_DUPLICATE_INTERFACE!>I<Int64><!> {} // error

extend<T> CPointer<T> <: I<T> {} // error

main() {}
