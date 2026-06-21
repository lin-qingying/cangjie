

interface I1<T> {
    func i1(a: Int32): Unit
    func i2(a: T): Unit
}

interface I2 <: I1<Int32> {}

interface I3 <: I1<Int64> {}

class C1 <: I1<Int32> {
    public func i1(a: Int32): Unit {}
    public func i2(a: Int32): Unit {}
}

class C2 <: I1<Int64> {
    public func i1(a: Int32): Unit {}
    public func i2(a: Int64): Unit {}
}

main() {
    return 0
}
