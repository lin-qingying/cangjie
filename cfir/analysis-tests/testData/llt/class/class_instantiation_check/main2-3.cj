

interface I1<T> {
    func i1(a: Int32): Unit
    func i1(a: T): Unit
}

interface I2<T> <: I1<T> {}

open class C1<T> <: I1<T> {
    public func i1(a: Int32): Unit {}
    public func i1(a: T): Unit {}
}

interface I3 <: I2<Int64> {}

class C2 <: C1<Int64> {}

main() {
    var a = C1<Int64>()
    return 0
}
