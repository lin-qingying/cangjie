

func run(): Int64 {
    var _: Int64 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 62 + -2 ** 62<!> - 1
    var _: Int64 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 63<!>

    var _ = UInt64(<!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 63<!> + <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 63<!> - 1)
    var _ = UInt64(<!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 64<!>)
    return 0
}

main(): Int64 {
    return run();
}
