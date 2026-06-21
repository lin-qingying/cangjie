

interface I1 {
    func f(): Int64
}

interface I2 <: I1 {
    func g(): Int64
}

class C {}

extend C <: I1 {
    public func f(): Int64 {
        1
    }
}

extend C <: I2 {
    public func g(): Int64 {
        2
    }
}

main() {
    var c = C()
    return c.f() + c.g() - 3
}
