


interface I {}

class A<X> {}

extend<V> A<V> <: I where V <: I {}

func f<<!MULTIPLE_CLASS_UPPER_BOUNDS!>T<!>>() where T <: A<T> & A<Int64> {}

main() {}
