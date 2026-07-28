

struct S {
    func foo(): Int {
        1
    }
}

interface I2 {
    func foo(): String
}

func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(x: T) where T <: S & I2 {
}

class A<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: S & I2 {}

main() {
}
