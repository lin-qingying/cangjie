

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Int64 & Countable<T> & Comparable<T> & Equatable<T> {}

main(): Int64 {
    return 0
}
