

// UpperBound: rule 3: If there are multiple classes then these classes, they must be in one inheritance chain.

open class Base {}

class Balala {}

func foo<<!MULTIPLE_CLASS_UPPER_BOUNDS!>T<!>>() where T <: Base & Balala {}

main(): Int64 {
    return 1
}
