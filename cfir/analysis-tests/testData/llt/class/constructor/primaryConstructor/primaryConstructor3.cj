

class D {
    public D(var D!: Int64 = 3) {}
}

class E {
    public E(var E: Int64) {}
}

class F {
    public F(var E: Int64) {}
    public func F(F: Int64) {
        E
    }
}

main(): Int64 {
    if (D().D == 3 && E(2).E == 2 && F(4).F(1) == 4) {
        return 0
    }
    return 1
}
