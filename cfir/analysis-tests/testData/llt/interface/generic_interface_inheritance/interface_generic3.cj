


interface I<T> {
    func foo(a: T): Int32
}

interface II<T> <: I<T> & I<Int32> & <!SUPER_TYPES_DUPLICATE!>I<Int32><!> {
    func foo(a: Int32): Int32
    func foo(a: T): Int32
}

main() {
    return 0
}
