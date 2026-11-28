// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：花括号 lambda 缺少 `=>`。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_double_arrow_in_lambda "expected '=>' in lambda expression, found keyword 'let'"。
func lambdaWithoutArrow(): Int64 {
    let f = {
        let y = 1
        y
    }
    return f()
}
