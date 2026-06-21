

class C {}

interface I1 {}

interface I2 {}

struct R {}

extend R <: I1 & I2 {}

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: R & I1 & I2 {}

main(): Int64 {
    return 0
}
