

// UpperBound: rule 2: There can not be two or more class-irrelevant upper bounds.

open class Base {}

class Child <: Base {}

func bar2<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: () -> Base & () -> Child {}

func goo2<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Base, Base) & (Child, Child) {}

main(): Int64 {
    return 0
}
