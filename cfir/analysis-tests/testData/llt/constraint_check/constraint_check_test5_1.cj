

// UpperBound: Tuple Type

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Int32, Int32) {
    return 0
}

open class Base {}

class Child <: Base {}

func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Int32, Base) {
    return 0
}

main(): Int64 {
    foo<(Int32, Int32)>()
    bar<(Int32, Base)>()
    bar<(Int32, Child)>() // Tuple type is covariant
}
