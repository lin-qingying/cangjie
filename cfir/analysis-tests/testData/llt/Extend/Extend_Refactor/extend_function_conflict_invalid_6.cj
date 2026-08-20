


interface I1 {
    func foo(): Int64 {
        1
    }
}

interface I2 {
    func foo(): Int32 {
        2
    }
}

class C1 {}

extend C1 <: I1 & I2 {
    public func <!RETURN_TYPE_INCOMPATIBLE!>foo<!>(): Int32 {
        2
    }
}

main() {
    return -1
}
