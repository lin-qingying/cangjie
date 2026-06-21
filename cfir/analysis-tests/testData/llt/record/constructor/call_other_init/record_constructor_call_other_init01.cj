

struct A {
    public let a: Int64
    public let b: Int64
    public let c: Int64
    public init() {
        this(0, 0, 0)
    }
    public init(x: Int64) {
        this(x, 0, 0)
    }
    public init(x: Int64, y: Int64) {
        this(x, y, 0)
    }
    public init(x: Int64, y: Int64, z: Int64) {
        this.a = x
        this.b = y
        this.c = z
    }
}

main(): Int64 {
    0
}
