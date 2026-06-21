


main(): Int64 {
    var a: Int8 = 1
    a <<= <!CONST_EVAL_SHIFT_COUNT_OVERFLOW!>8<!>
    return 0
}
