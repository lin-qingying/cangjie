


open class A {}

open class B {}

class C <: A & <!ILLEGAL_MULTI_INHERITANCE!>B<!> {}

interface I {}

class D <: I & <!SUPERCLASS_MUST_BE_PLACED_AT_FIRST!>A<!> {}
