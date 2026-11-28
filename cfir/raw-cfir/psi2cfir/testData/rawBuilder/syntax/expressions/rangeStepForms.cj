// RUN_PIPELINE_TILL: FRONTEND
// 区间与步长：`..` / `..=`、步长 `a..b:step`（官方 ParseExpr.cpp：COLON 后接表达式，1.0.5 起支持）、步长变量、切片里的步长、Range 的 step 属性。
// 注意：区间不能链式（`1..10..2` 是 parse 错误）、区间不能缺起点或终点、字符串不是 Countable（`"a".."b"` 报 sema_generic_type_argument_not_match_constraint）、
// 没有 `step()` 自由函数（步长是 `:` 后缀表达式）。
// 官方依据：官方 ParseExpr.cpp ParseRangeExpr / RANGEOP / CLOSEDRANGEOP；cjc 1.0.5 / 1.1.3 零诊断。
func rangeStepForms(xs: Array<Int64>): Int64 {
    var t = 0
    for (i in 1..10) {
        t += i
    }
    for (i in 1..=10) {
        t += i
    }
    for (i in 1..10:2) {
        t += i
    }
    for (i in 1..=10:3) {
        t += i
    }
    let st = 2
    let a = 1..10:st
    let b = xs[0..10:2]
    let c = xs[0..=10:2]
    let d = (1..10).step
    let _ = (a, b, c, d)
    return t
}
