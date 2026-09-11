interface Base {}
open class Parent <: Base {}
class Child <: Parent {}

func linked<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>, Y>(value: X): Unit where X <: Y, Y <: Base {}
func mixed<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>, Y>(value: X): Unit where X <: Base & Y, Y <: Base {}
func cycle<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>X<!>, Y>(value: X): Unit where X <: Y, Y <: X {}

func uses(): Unit {
    linked(Child())
    mixed(Child())
    cycle(1)
    linked<Child, Parent>(Child())
    linked<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Parent<!>, Child>(Parent())
}
