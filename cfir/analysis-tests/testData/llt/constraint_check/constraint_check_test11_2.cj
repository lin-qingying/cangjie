

interface I {}

struct R <: I {}

func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>U<!>>() where T <: U & R, U <: R {}

main(): Int64 {
    return 0
}
