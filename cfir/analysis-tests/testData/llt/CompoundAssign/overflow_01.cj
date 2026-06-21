

main(): Int64 {
    var a: Int8 = 1
    var b: Int8 = 2
    var c: Int8 = 4
    var d: Int8 = 9
    a <<= <!LITERAL_NUMERIC_OVERFLOW!>128<!>
    a >>= <!LITERAL_NUMERIC_OVERFLOW!>128<!>
    a <<= <!CONST_EVAL_NEGATIVE_SHIFT_COUNT!>-1<!>
    a >>= <!CONST_EVAL_NEGATIVE_SHIFT_COUNT!>-1<!>
    a += <!LITERAL_NUMERIC_OVERFLOW!>128<!>
    b -= <!LITERAL_NUMERIC_OVERFLOW!>128<!>
    c *= <!LITERAL_NUMERIC_OVERFLOW!>128<!>
    d /= <!LITERAL_NUMERIC_OVERFLOW!>128<!>
    return 1
}
