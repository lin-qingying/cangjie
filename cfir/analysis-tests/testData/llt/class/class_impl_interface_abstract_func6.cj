

interface I1 {
    func f1(): Int32
}

interface I2 {
    func f1(): Int32 {
        8
    }
}

class C <: I1 & I2 {
    public func f1(): Int32 {
        2
    }
}
