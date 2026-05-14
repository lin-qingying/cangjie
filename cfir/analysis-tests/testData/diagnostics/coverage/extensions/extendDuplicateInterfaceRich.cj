// RUN_PIPELINE_TILL: FRONTEND
class Box {}
interface Display {}
interface Hashable {}

extend Box <: Display & <!EXTEND_DUPLICATE_INTERFACE!>Display<!> & Hashable {}
extend Box <: Hashable & <!EXTEND_DUPLICATE_INTERFACE!>Hashable<!> {}
