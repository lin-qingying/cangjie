


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

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C1<!> <: I2 & I3 {}

main(): Int64 {
    return 0
}
