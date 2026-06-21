

interface A<T> {
    static func foo(a: T): T {
        return a
    }
}

extend Int32 <: A<Int64> {}

main() {
    let b: Int64 = 1
    return Int32.foo(b) - 1
}
