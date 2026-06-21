

class C {}

interface I {}

struct R <: I {}

func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Bool & C {}

func goo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Bool & R & C {}

func noo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: () -> R & C {}

func doo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (R, R) & C {}

main(): Int64 {
    return 1
}
