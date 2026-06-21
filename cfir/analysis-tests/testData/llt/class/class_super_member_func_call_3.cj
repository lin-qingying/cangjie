

interface I0 {
    func foo(): Int64 {
        return 0
    }
}

class C0 <: I0 {
    public func foo1(): Int64 {
        var res: Int64 = foo()
        return res
    }
}

main(): Int64 {
    var obj = C0()
    var x = obj.foo1()
    if (x == 0) {
        return 0
    }
    return 1
}
