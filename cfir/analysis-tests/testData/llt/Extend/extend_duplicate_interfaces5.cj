

interface I<T> {}

class A<T> <: I<T> {}

extend<T> A<T> <: <!EXTEND_DUPLICATE_INTERFACE!>I<T><!> {}

main() {
    return 0
}
