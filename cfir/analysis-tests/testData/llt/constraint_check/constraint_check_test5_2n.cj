


// UpperBound: Tuple Type

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Int32, Int32) {
    return 0
}

open class Base {}

class Child <: Base {}

class C {}

func bar<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Int32, Child) {
    return 0
}

main(): Int64 {
    foo<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>(Int32, Int64)<!>>()
    bar<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>(Int32, Base)<!>>()
    bar<(Int32, Child)>() // OK
    bar<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>(Int32, C)<!>>()
    bar<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>(Int64, Child)<!>>()
    bar<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>(C, Child)<!>>()
}

