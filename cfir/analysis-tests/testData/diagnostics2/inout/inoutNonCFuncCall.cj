// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：普通 Cangjie 函数调用上使用 inout 实参 →
// error: 'inout' can only be used in a 'CFunc' calling（kind sema_inout_can_only_used_in_cfunc_calling，锚 `inout v` 整段）。
// CFIR CfirInoutSemanticsChecker 对非 CFunc 调用上的 inout 实参报 INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING，锚在被修改的实参表达式 `v` 上。
func norm(x: Int64): Unit {
    x
}

func useNonCFunc(): Unit {
    var v = 1
    norm(<!INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING!>inout v<!>)
}
