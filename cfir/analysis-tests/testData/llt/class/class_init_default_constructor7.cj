

open class A {
    public var a: Int64
    public init(b: Int64) {
        a = b
    }
    public init() {
        a = 0
    }
}

class X <: A {
    public let c: Int64 = 0
}

main(): Int64 {
    return 0
}
