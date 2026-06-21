


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

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend C1 <: I1 & I2 {}

main() {
    return -1
}
