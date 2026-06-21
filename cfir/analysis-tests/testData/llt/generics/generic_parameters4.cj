


open class A<T, V> {}

class B<T> <: <!GENERIC_ARGUMENT_NO_MATCH!>A<T><!> {}

class C<T> <: <!GENERIC_ARGUMENT_NO_MATCH!>A<T><!> {}

main() {
    return 0
}
