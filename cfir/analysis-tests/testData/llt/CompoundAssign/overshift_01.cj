


main(): Int64 {
    var a: Int8 = 1
    a <<= <!CONST_EVAL_SHIFT_COUNT_OVERFLOW!>9<!>
    a >>= <!CONST_EVAL_SHIFT_COUNT_OVERFLOW!>9<!>
    return 1
}
