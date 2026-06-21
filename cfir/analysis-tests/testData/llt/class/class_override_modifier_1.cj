

interface I1 {
    func foo(): Int64
}

interface I2 {
    func foo(): Int64 {
        return 1
    }
}

open class C1 {
    public open func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 & I1 & I2 {
    public override func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    var res = C2().foo()
    if (res == 3) {
        return 0
    }
    return 1
}
