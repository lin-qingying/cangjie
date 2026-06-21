


interface I<T> {
    func foo(a: T): Int32
}

interface II<T> <: I<T> & I<Int32> & I<Float64> {
    func foo(a: Int32): Int32
    func foo(a: T): Int32
    func foo(a: Float64): Int32
}

main() {
    return 0
}
