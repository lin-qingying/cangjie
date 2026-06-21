


//brief:IntNative赋值为常量计算且溢出，编译报错
main(): Int64 {
    var _: IntNative = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>-9223372036854775808 - 1<!>
    var _: IntNative = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>9223372036854775807 + 1<!>
    return 0
}
