


interface I1 {
    prop foo: Int64 {
        get() {
            return 1
        }
    }
}

interface I2 {
    prop foo: Int64 {
        get() {
            return 2
        }
    }
}

class C1 {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend C1 <: I1 & I2 {}

main() {
    return -1
}
