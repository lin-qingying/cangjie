

open class C1 {
    public func foo(): Int64 {
        return 0
    }
}

interface I1 {
    func foo(): Int64 {
        return 0
    }
}

class C2 <: C1 & I1 {
    public func foo1(): Int64 {
        var f = super.foo
        f()
        return 0
    }
}

main(): Int64 {
    var obj = C2()
    if (obj.foo1() == 0) {
        return 0
    }
    return 1
}
