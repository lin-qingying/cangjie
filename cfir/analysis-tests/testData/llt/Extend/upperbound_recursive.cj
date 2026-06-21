


class A<X> {}

interface I {}

extend<V> A<V> <: I where V <: I {}

func f<T>() where T <: A<T> {}

main(): Int64 {
    return 0
}
