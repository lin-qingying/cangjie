

func Add<T>(a: T, b: T): T where <!UNRESOLVED_REFERENCE!>U<!> <: <!UNRESOLVED_REFERENCE!>Num<!> {
    return a <!INVALID_BINARY_OPERATOR!>+<!> b
}

func g<T>() where T <: <!UNRESOLVED_REFERENCE!>update<!> {}

<!CONFLICTING_OVERLOADS!>func g<!><<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Int64 {}
