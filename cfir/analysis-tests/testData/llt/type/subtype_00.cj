


interface I {}

class A<X> {}

extend<V> A<V> <: I where V <: I {}

func f<<!ONLY_ONE_CLASS_BOUND_ALLOWED!>T<!>>() where T <: A<T> & A<Int64> {}

main() {}
