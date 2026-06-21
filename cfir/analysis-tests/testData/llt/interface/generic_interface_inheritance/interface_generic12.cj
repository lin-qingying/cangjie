


interface I<T> {
    func foo(a: T): T
}

interface II<T> <: I<Int32> {
    func foo(a: Int32): Int32
}

interface III <: II<Bool> {
    func foo(a: Bool): Bool
}

main() {
    return 0
}
