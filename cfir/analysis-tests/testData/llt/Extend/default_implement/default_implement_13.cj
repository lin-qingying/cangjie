


interface I1 {
    func foo() {}
}

interface I2 {
    func foo() {}
}

class A {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> A <: I1 {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> A <: I2 {}

main() {}
