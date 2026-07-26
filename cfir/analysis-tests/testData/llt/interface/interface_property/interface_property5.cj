


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

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED, INHERIT_MEMBER_TYPE_INCONSISTENT!>C<!> <: I1 & I2 {}

main() {
    return 0
}
