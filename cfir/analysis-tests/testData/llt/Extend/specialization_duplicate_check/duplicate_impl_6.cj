


interface I<T> {}

class A<K> {}

extend A<(Int64, Int64, Int64)> <: I<Int64> {}

extend<T> A<(T, T) -> T> <: I<T> {}

main() {}
