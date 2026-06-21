

// Error reported on S <: I2. Constraint of 'func bar' should not report error.
struct S <: I2 {
    func foo(): Int {
        1
    }
}

interface I2 {
    func foo(): String
}

func bar<T>(x: T) where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>S<!> & I2 {
}

class A<T> where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>S<!> & I2 {}

main() {
}
