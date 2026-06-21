

interface I<T> {}

class A<T> <: I<Int32> {}

extend<T> A<T> <: I<Int64> {}

main() {
    return 0
}
