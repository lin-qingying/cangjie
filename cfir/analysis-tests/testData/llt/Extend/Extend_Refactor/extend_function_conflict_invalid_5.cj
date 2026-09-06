


interface I1 {
    func foo(): Int64 {
        0
    }
}

interface I2 <: I1 {
    func foo(): Int64 {
        1
    }
}

class C1 {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> C1 <: I2 & I1 {}

main() {
    return -1
}
