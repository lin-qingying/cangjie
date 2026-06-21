


interface I1 {
    func foo() {}
}

interface I2 <: I1 {}

class A {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend A <: I1 & I2 {} // failed

main() {
    A().foo()
}
