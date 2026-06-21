


interface I1 {
    func foo() {}
}

interface I2 {
    func foo() {}
}

class A {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend A <: I1 {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend A <: I2 {}

main() {}
