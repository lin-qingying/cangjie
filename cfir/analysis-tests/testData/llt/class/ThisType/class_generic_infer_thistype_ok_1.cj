

open class C1<T> {
    public func foo1() {
        return this
    }
}

class C2<T> <: C1<T> {
    public func foo2() {
        return this
    }
}

main() {
    let obj = C2<Int64>()
    obj.foo1().foo2()
    return 0
}
