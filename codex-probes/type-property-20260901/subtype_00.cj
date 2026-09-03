


interface I {}

class A<X> {}

extend<V> A<V> <: I where V <: I {}

func f<T>() where T <: A<T> & A<Int64> {}

main() {}
