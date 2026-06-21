

extend<T> Array<T> {
    static func foo(a: T): T {
        return a
    }
}

main() {
    return Array<Int64>.foo(1) - 1
}
