// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：lambda 形参写成 `b!:`（官方 ParseLambdaExpr 不解析 `!`，默认值 / 具名实参属于函数声明形参，不属于 lambda 形参）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_double_arrow_in_lambda "expected '=>' in lambda expression, found '!'"。
func lambdaNamedParameter(): Unit {
    let g = { a: Int64, b!: Int64 => a + b }
    let _ = g(1, b: 2)
}
