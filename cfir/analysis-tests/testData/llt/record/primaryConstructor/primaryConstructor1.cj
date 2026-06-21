

struct B<X, Y> {
    public B(x: Int64, y: X, public var z: Y) {}
    public init(z: Y) {
        this.z = z
    }
}

struct C {
    public C(var z!: Int64 = 3) {
    }
}

main(): Int64 {
    B<Int64, Int64>(4).z
    if (B<Int64, Int64>(4).z == 4 && B<Int64, String>(1, 2, "str").z == "str" && C().z == 3) {
        return 0
    }
    return 1
}
