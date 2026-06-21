func test<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(a : T) : Bool where T <: Array<Bool> {
    if (a.isEmpty()) {
        return false
    }
    return a[0]
}

main() {
    if (!test([true])) {
        return 1
    }
    return 0
}


