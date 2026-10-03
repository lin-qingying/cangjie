// RUN_PIPELINE_TILL: FRONTEND
// 多元赋值：`(a, b) = (b, a)` 交换、元组变量整体赋值 `t = (3, 4)`。
// 官方依据：官方 Sema/TypeCheckExpr/AssignExpr.cpp CheckMultipleAssignExpr（保留元组结构，不在解析期脱糖）；cjc 1.0.5 / 1.1.3 零诊断。
func multipleAssignForms(): Unit {
    var a = 1
    var b = 2
    (a, b) = (b, a)
    var t: (Int64, Int64) = (1, 2)
    t = (3, 4)
    let _ = (a, b, t)
}
