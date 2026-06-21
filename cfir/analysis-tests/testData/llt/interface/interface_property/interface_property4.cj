


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

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C<!> <: I1 & I2 {}

main() {
    return 0
}
