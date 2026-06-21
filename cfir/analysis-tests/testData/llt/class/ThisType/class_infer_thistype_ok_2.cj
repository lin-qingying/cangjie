

open class C1 {
    public func foo1() {
        return this
    }
}

class C2 <: C1 {
    public func foo2() {
        return this
    }
}

main() {
    let obj = C2()
    obj.foo2().foo1()
    return 0
}
