

open class C1<T> {
    public func c1(a: Int32) {
        return 0
    }
    public func c2(a: T) {
        return 0
    }
}

class C2 <: C1<Int32> {}

class C3 <: C1<Int64> {}

main() {
    var a = C1<Int32>()
    var b = C1<Int64>()
    return 0
}
