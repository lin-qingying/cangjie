// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
foreign func cFunc(value: Int64): Unit

func passLet(): Unit {
    let x = 1
    <!INOUT_MUST_BE_VAR_VARIABLE!>/* suggested message: inout argument must be a var variable. source: manual_source_zh_cn_cangjie_c_inout_参数_3; docs/diagnostics-gap-vs-official-cpp-sema-detailed.md#123-sema_inout_must_be_var_variable */cFunc(inout x)<!>
}

func passVar(): Unit {
    var x = 1
    cFunc(inout x)
}
