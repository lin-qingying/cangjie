


open class A<T> {}

class B<T, V> <: <!GENERIC_ARGUMENT_NO_MATCH!>A<T, V><!> {}

class C<T, V> <: <!GENERIC_ARGUMENT_NO_MATCH!>A<V, T><!> {}

class D<T1, T2, T3> <: <!GENERIC_ARGUMENT_NO_MATCH!>A<T1, T2, T3><!> {}

main() {
    return 0
}
