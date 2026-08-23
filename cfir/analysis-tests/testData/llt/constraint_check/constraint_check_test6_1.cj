



// UpperBound: Array Type

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Array<Int32> {
    return 0
}

open class Base {}

class Child <: Base {}

func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Array<(Int32, Base)> {
    return 0
}

func jay<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Int32, Array<Base>) {
    return 0
}

main(): Int64 {
    foo<Array<Int32>>()
    bar<Array<(Int32, Base)>>()
    bar<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Array<(Int32, Child)><!>>()
    jay<(Int32, Array<Base>)>()
    jay<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>(Int32, Array<Child>)<!>>()
}

