


interface I1 {
    func foo1() {
        return 0
    }
}

struct R1 {}

extend R1 <: I1 {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func foo1() {
        return 0
    }
}

interface I2 {
    mut func foo2() {
        return 0
    }
}

struct R2 {}

extend R2 <: I2 {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic func foo2() {
        return 0
    }
}

interface I3 {
    func foo3(): Unit
}

struct R3 {}

extend R3 <: I3 {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func foo3(): Unit {
    }
}

interface I4 {
    mut func foo4(): Unit
}

struct R4 {}

extend R4 <: I4 {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic func foo4(): Unit {
    }
}

main() {
    return 1
}
