


struct A<T> {
    public let x: (T) -> T = {input: T => input}
    public let y: (Int64) -> Int64 = {input: Int64 => input}
}

main() {
    var a = A<Unit>()
    return 0
}
