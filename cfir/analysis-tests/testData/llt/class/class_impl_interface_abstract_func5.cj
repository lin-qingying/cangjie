

interface I1 {
    func f1(): Int32
    func f2(): Int32 {
        0
    }
}

interface I2 {
    func f1(): Int32 {
        0
    }
    func f2(): Int32 {
        0
    }
}

class C <: I1 & I2 {
    public override func f2(): Int32 {
        0
    }
}

main(): Int64 {
    return Int64(C().f2())
}
