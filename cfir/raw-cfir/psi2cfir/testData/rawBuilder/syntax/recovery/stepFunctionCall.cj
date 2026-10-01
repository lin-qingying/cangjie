// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：把步长写成自由函数 `step(1..10, 2)` 或元组 `(1..10, 2)`（官方没有 step 自由函数：步长是区间后的 `:` 后缀表达式；元组形式报
// sema_expr_in_forin_must_has_iterator "the type Tuple<Range<Int64>, Int64> of expression in for-in must have an iterator"）。
// 官方 cjc 1.0.5 / 1.1.3：sema_undeclared_identifier 'step' / sema_expr_in_forin_must_has_iterator。
func stepFunctionCall(): Int64 {
    var t = 0
    for (i in step(1..10, 2)) {
        t += i
    }
    for (i in (1..10, 2)) {
        t += i
    }
    return t
}
