

interface I1 {
    func foo(): Int64 {
        return 1
    }
}

class C1 <: I1 {
    public func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    var a: Int64 = C1().foo()
    if (a == 2) {
        return 0
    }
    return 1
}
