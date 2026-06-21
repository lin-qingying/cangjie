

var x: Int64 = 0

open class C1 {
    public func foo1(): This {
        x = 1
        return this
    }
}

class C2 <: C1 {
    public func foo2(): This {
        x = 2
        return this
    }
}

main() {
    let obj = C2()
    obj.foo2().foo1()
    if (x == 1) {
        return 0
    }
    return 1
}
