


interface I<T> {}

class A<K, R> {}

extend A<Int64, String> <: I<Int64> {}

extend<T> A<T, T> <: I<T> {}

main() {}
