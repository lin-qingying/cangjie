// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：区间运算链式书写 `1..10..2`（官方 parse_chained_none_associative "range operators cannot be chained"）。步长必须写成 `1..10:2`。
// 官方 cjc 1.0.5 / 1.1.3：parse_chained_none_associative。
func rangeChained(): Unit {
    let a = 1..10..2
    let _ = a
}
