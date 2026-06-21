

interface I {}

type X = I

interface <!SUPER_TYPES_DUPLICATE!>J<!> <: I & X {}

main() {}
