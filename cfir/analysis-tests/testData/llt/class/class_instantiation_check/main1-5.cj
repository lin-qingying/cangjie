

open class C1<T> {
    public func c1(a: Int32) {
        return 0
    }
    public func c1(a: T) {
        return 0
    }
}

open class C2<T> <: C1<T> {}

class C3 <: C2<Int64> {}

main() {
    var a = C1<Int64>()
    var b = C2<Int64>()
    return 0
}
