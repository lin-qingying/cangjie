

class C {}

interface I {}

struct R {}

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: R & I {}

main(): Int64 {
    return 1
}
