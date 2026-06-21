

// UpperBound: constraint sanity check when upperbound has other generic parameter

struct R {}

open class Base {}

class Child <: Base {}

interface I1 {}

interface I2 {}

extend Int32 <: I1 {}

func doo<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>T<!>, U, Z>() where T <: U, U <: Z, Z <: T {}

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Z<!>>() where T <: U & R, U <: Z, Z <: R {}

func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>, Z>() where T <: U & Child, U <: Z, Z <: Base {}

func goo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>, Z>() where T <: U & I1 & I2, U <: Z, Z <: Base {}

func noo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Z<!>>() where T <: U & I1, U <: Z, Z <: Int32 {}

main(): Int64 {
    return 0
}
