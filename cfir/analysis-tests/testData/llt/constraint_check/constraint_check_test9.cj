

// UpperBound: rule 2: There can not be two or more class irrelevant upper bounds.

open class Base {}

class Balala {}

func foo1<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Int32 & Bool {}

func bar1<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: () -> Int32 & () -> Bool {}

func goo1<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Int32, Bool) & (Int64, Bool) {}

func bar2<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: () -> Base & () -> Balala {}

func goo2<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Base, Bool) & (Balala, Bool) {}

main(): Int64 {
    return 1
}
