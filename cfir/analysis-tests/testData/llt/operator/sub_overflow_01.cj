

func run() {
    println(<!CONST_EVAL_ARITHMETIC_OVERFLOW!>127i8 - -128i8<!>)
    // 取负运算自身的溢出报在运算上，不是报在字面量上。
    // 官方 cjc 1.0.5 探针（`var _` 带显式标注的丢弃形态）：
    // `-(-9223372036854775808)` 报 `arithmetic operation '-' overflow`，
    // 锚点覆盖整个 `-(...)`；`-(-9223372036854775807)` 通过。
    // 注意：若该变量未被使用，初始化器是死代码，官方不做常量求值而不报诊断。
    var _: Int64 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>-(-9223372036854775808)<!>
    var _: Int8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>-(-128i8)<!>
    var _: Int64 = -(-9223372036854775807)
}

main() {
    run()
}
