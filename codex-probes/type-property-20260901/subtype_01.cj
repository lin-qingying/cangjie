


interface I {}

class A<T> <: I where T <: A<T> & A<Int64> {}

main() {}
