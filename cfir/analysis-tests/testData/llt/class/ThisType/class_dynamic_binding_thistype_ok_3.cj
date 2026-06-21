

open class C1 {
    public var a: Int64 = 1
    public func foo1() {
        return this
    }
}

class C2 <: C1 {
    public var b: Int64 = 2
    public func foo2() {
        return this
    }
}

main() {
    let obj = C2()
    var f1 = obj.foo1
    var xx = f1()
    var yy = xx.b

    if (yy == 2) {
        return 0
    }

    return 1
}
