


interface I1<T> {}

interface I2<T> <: I1<T> {}

class A<T> <: I2<T> {}

extend A<String> <: <!EXTEND_DUPLICATE_INTERFACE!>I1<String><!> {} // error

main() {}
