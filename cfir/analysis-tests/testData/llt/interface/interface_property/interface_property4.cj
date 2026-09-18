


interface I1 {
    prop a: Int32 {
        get() {
            return 0
        }
    }
}

interface I2 {
    prop a: Int32 {
        get() {
            return 3
        }
    }
}

class <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>C<!> <: I1 & I2 {}

main() {
    return 0
}
