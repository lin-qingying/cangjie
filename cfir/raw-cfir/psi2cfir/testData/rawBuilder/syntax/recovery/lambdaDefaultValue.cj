// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：lambda 形参写默认值（`{ a: Int64, b: Int64 = 2 => ... }`，官方默认值只属于具名形参 `b!:`，且 lambda 形参不接受 `!`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_double_arrow_in_lambda "expected '=>' in lambda expression, found '='"。
func lambdaDefaultValue(): Unit {
    let g = { a: Int64, b: Int64 = 2 => a + b }
    let _ = g(1)
}
