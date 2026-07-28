

// Error reported on S <: I2. Constraint of 'func bar' should not report error.
struct S <: I2 {
    func <!RETURN_TYPE_INCOMPATIBLE!>foo<!>(): Int {
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
