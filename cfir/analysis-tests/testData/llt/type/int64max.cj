


func run(): Int64 {
    var i1: Int64 = -9223372036854775808
    var i2: Int64 = - 9223372036854775808
    var i3: Int64 = <!LITERAL_NUMERIC_OVERFLOW!>- 9223372036854775809<!>
    var i4: Int64 = -0x8000000000000000
    var i5: Int64 = <!LITERAL_NUMERIC_OVERFLOW!>-0x8000000000000001<!>
    // 括号使内层恢复为正字面量并按其值域判断：官方 cjc 1.0.5 探针
    // `var c: Int64 = -(9223372036854775808)` 报 LITERAL_NUMERIC_OVERFLOW，
    // 锚点只覆盖内层正字面量、不含 `-` 与括号。
    var i6: Int64 = -(9223372036854775807)
    var i7: Int64 = -(<!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>)
    return 0
}

main(): Int64 {
    return run()
}
