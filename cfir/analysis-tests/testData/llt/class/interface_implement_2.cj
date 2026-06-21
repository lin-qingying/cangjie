

interface I1 {
    func foo(): Int64
}

interface I2 <: I1 {
    func foo(): Int64 {
        return 2
    }
}

interface I3 <: I1 {
    func foo(): Int64 {
        return 3
    }
}

class C1 <: I2 & I3 {
    public func foo(): Int64 {
        return 1
    }
}

main(): Int64 {
    var res = C1().foo()
    if (res == 1) {
        return 0
    }
    return 1
}

