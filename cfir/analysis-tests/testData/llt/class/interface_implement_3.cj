

interface I1 {
    func foo(): Int64
}

interface I2 {
    func foo(): Int64
}

class C1 <: I1 & I2 {
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

