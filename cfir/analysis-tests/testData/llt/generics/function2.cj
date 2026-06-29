

func Add<T>(a: T, b: T): T where <!UNDECLARED_TYPE_NAME!>U<!> <: <!UNDECLARED_TYPE_NAME!>Num<!> {
    return a <!INVALID_BINARY_OPERATOR!>+<!> b
}

func g<T>() where T <: <!UNDECLARED_TYPE_NAME!>update<!> {}

func <!CONFLICTING_OVERLOADS!>g<!><<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Int64 {}
