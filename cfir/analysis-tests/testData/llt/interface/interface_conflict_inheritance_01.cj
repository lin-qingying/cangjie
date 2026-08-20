


class C {}

class D {}

class E {}

interface I0 {
    func foo(): C
}

interface I1 {
    func foo(): D
}

interface I2 {
    func foo(): E
}

interface <!INHERIT_MEMBER_TYPE_INCONSISTENT!>II<!> <: I0 & I1 & I2 {}

interface III <: I1 & I2 {
    func <!RETURN_TYPE_INCOMPATIBLE!>foo<!>(): E
}

main() {
    return 0
}
