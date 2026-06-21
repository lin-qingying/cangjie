

interface I {}

struct R <: I {}

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>>() where T <: U & R, U <: Int32 {}

main(): Int64 {
    return 1
}
