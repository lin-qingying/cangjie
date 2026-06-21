

class A {
    public var a: Int64
    public var b: Int64
    public init(n: Int64) {
        this(n, n)
        a = n
    }
    public init(n: Int64, m: Int64) {
        a = n
        b = m
    }
}

main(): Int64 {
    return 0
}
