


func test<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(a: T) where T <: (Int64) -> Int64 {
    a(1)
}

main(): Int64 {
    return test {i => i}
}
