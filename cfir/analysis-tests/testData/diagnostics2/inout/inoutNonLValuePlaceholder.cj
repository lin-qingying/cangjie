// RUN_PIPELINE_TILL: FRONTEND
// 范围口径：INOUT_MUST_BE_VAR_VARIABLE 与同族 INOUT_MODIFY_* 一致，官方对 `inout` 限定诊断锚
// `inout X` 整段（refactor kind，见 inoutCString.cj 取证），标记覆盖整段实参（含 inout 关键字）。
foreign func cFunc(value: Int64): Unit

func passLiteral(): Unit {
    unsafe {
        cFunc(<!INOUT_MUST_BE_VAR_VARIABLE!>inout 1<!>)
    }
}

func passExpression(): Unit {
    var x = 1
    unsafe {
        cFunc(<!INOUT_MUST_BE_VAR_VARIABLE!>inout (x + 1)<!>)
    }
}
