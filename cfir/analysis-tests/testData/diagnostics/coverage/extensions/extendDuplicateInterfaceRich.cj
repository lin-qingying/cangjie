class Box {}
interface Display {}
interface Hashable {}

extend Box <: Display & <!EXTEND_DUPLICATE_INTERFACE!>Display<!> & <!EXTEND_DUPLICATE_INTERFACE!>Hashable<!> {}
extend Box <: <!EXTEND_DUPLICATE_INTERFACE!>Hashable<!> & <!EXTEND_DUPLICATE_INTERFACE!>Hashable<!> {}
