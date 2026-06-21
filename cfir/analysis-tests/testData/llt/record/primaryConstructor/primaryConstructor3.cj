

struct D {
    public D(var D!: Int64 = 3) {}
}

struct E {
    public E(var E: Int64) {}
}

struct F {
    public F(let E: Int64) {}
    public func F(a: Int64) {
        E
    }
}

main(): Int64 {
    if (D().D == 3 && E(2).E == 2 && F(4).F(2) == 4) {
        return 0
    }
    return 1
}
