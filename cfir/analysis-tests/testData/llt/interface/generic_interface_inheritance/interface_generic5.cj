


interface I<T> {
    func foo(a: T): Int32
}

interface II<T> <: I<T> & <!SUPER_TYPES_DUPLICATE!>I<T><!> & I<Int32> {
    func foo(a: Int32): Int32
    func foo(a: T): Int32
}

main() {
    return 0
}
