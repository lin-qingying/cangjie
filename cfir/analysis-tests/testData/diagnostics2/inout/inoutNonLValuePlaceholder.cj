// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
foreign func cFunc(value: Int64): Unit

func passLiteral(): Unit {
    <!INVALID_INOUT_ARGUMENT!>/* suggested message: inout argument must be a mutable l-value. source: manual_source_zh_cn_cangjie_c_inout_参数_3; docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */cFunc(inout 1)<!>
}

func passExpression(): Unit {
    var x = 1
    <!INVALID_INOUT_ARGUMENT!>/* suggested message: inout argument must be a mutable l-value. source: manual_source_zh_cn_cangjie_c_inout_参数_3; docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */cFunc(inout (x + 1))<!>
}
