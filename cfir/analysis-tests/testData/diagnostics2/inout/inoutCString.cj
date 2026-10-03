// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   `foreign func getCString(): CString` + `var s = getCString()` + `consumeCString(inout s)`
//     → error: the expression qualified by 'inout' cannot be of type 'CString'（锚 `inout s` 整段）
//     官方 kind sema_inout_modify_cstring_or_zerosized（DiagRefactor/DiagnosticSema.def:178）
//   对照：`var s = "x"`（推断为 Struct-String）不是 CType，落到 CString 形参时官方给
//     `sema_inout_modify_non_ctype` + mismatched types（expected 'CString', found 'Struct-String'），
//     见 inoutNonCType.cj；本用例要求实参本身已是 CString 类型。
foreign func getCString(): CString
foreign func consumeCString(p: CString): Unit

func inoutOnCString(): Unit {
    unsafe {
        var s = getCString()
        consumeCString(<!INOUT_MODIFY_CSTRING_OR_ZEROSIZED!>inout s<!>)
    }
}
