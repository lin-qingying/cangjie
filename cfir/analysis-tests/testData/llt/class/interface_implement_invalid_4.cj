


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

class <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>C1<!> <: I2 & I3 {}

main(): Int64 {
    return 0
}
