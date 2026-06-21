

open class A {
    public var a: Int64
    public init(n: Int64) {
        if (n > 0) {
            a = n
        } else {
            a = -n
        }
    }
}

main(): Int64 {
    return 0
}
