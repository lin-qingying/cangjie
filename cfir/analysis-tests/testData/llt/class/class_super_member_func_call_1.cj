

open class C0 {
    public func foo(): Int64 {
        return 0
    }
}

open class C1 <: C0 {}

class C2 <: C1 {
    public func foo2(): Int64 {
        var res: Int64 = super.foo()
        return res
    }
}

main(): Int64 {
    var obj = C2()
    var x = obj.foo2()
    if (x == 0) {
        return 0
    }
    return 1
}
