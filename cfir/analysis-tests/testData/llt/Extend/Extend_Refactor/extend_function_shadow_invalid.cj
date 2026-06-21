

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

extend C1 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>(): Int64 {
        3
    }
}

extend C1 <: I1 {}

extend C1 <: I2 {}

main() {
    return 0
}
