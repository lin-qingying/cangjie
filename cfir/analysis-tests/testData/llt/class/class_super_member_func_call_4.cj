

interface I0 {
    func foo(): Int64 {
        return 0
    }
}

interface I1 {
    func foo1(): Int64 {
        return 1
    }
}

open class C0 {
    public func foo2(): Int64 {
        return 2
    }
}

class C1 <: C0 & I0 & I1 {
    public func test(): Int64 {
        var res: Int64 = foo()
        return res
    }

    public func test1(): Int64 {
        var res: Int64 = foo1()
        return res
    }

    public func test2(): Int64 {
        var res: Int64 = super.foo2()
        return res
    }
}

main(): Int64 {
    var obj = C1()
    var x = obj.test()
    var y = obj.test1()
    var z = obj.test2()
    if (x == 0 && y == 1 && z == 2) {
        return 0
    }
    return 1
}
