// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：无名形参上写默认值（官方默认值只对具名形参 `x!` 有效）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_dot_lparen "expected ',' or ')', found '='"。
func defaultParamOnUnnamed(a: Int64 = 1, b!: Int64 = 2): Unit {
    let _ = (a, b)
}
