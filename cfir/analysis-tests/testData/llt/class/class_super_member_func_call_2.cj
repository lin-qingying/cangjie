

open class C0 {
    public open func foo(): Int64 {
        return 0
    }
}

open class C1 <: C0 {
    public override func foo(): Int64 {
        return 1
    }
}

class C2 <: C1 {
    public func foo2(): Int64 {
        var res: Int64 = super.foo()
        return res
    }
}

main(): Int64 {
    var obj = C2()
    var x = obj.foo2()
    if (x == 1) {
        return 0
    }
    return 1
}
