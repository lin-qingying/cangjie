

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Int32 {
    return 0
}

main(): Int64 {
    foo<Int32>()
}
