


open class A {}

class B <: A {}

interface I1 {
    prop a: A {
        get() {
            return A()
        }
    }
}

interface I2 {
    prop a: B {
        get() {
            return B()
        }
    }
}

class <!INHERIT_MEMBER_TYPE_INCONSISTENT, INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>C<!> <: I1 & I2 {}

main() {
    return 0
}
