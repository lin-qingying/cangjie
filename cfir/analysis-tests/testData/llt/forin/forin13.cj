


enum E {
    A | B(Int64)
}

main() {
    let arr = [A, B(0)]
    <!FORIN_PATTERN_MUST_BE_IRREFUTABLE!>for<!> (<!ENUM_PATTERN_PARAM_SIZE_ERROR!>E.A(x)<!> in arr) {}
}
