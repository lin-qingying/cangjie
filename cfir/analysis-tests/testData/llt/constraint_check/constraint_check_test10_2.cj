

interface I {}

interface I2 {}

struct R <: I {}

extend Int32 <: I {}

func goo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Int32 & I {} // OK, Int32 is the subtype of Interface I.

func noo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: R & I {} // OK, struct R is the subtype of interface I.

func foo<T>() where T <: I & I2 {} // OK

main(): Int64 {
    return 0
}
