


abstract class C {
    public func f(): Int64
}

interface I {
    func f(): Int64
}

extend C <: I {} // OK

class D <: C {
    public func f(): Int64 {
        11
    }
}

main(): Int64 {
    if (D().f() != 11) {
        return 1
    }
    return 0
}
