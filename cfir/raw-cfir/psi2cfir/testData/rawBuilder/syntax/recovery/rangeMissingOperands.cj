// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：区间缺起点（`..10`）或缺终点（`1..`）（官方 parse_expected_expression "expected expression after '=', found '..'" / "expected expression after '..'"）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_expression。
func rangeMissingOperands(): Unit {
    let a = ..10
    let b = 1..
    let _ = (a, b)
}
