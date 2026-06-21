

interface I1 {
    func foo(): Int64 {
        1
    }
}

interface I2 {
    func foo(): Int64 {
        2
    }
}

class C1 {}

extend C1 <: I1 & I2 {
    public func foo(): Int64 {
        3
    } // ok
}

main() {
    return 0
}
