

interface I<T> {}

class A<T> <: I<Int32> {}

extend<T> A<T> <: <!EXTEND_DUPLICATE_INTERFACE!>I<Int32><!> {}

main() {
    return 0
}
