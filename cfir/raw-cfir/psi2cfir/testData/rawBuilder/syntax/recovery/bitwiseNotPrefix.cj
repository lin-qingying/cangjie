// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：把 `~` 当作一元前缀操作符（`~a`）。官方解析器不接受：可重载的一元操作符只有 `-` 与 `!`（见
// declarations/operatorOverloadForms.cj），前缀表达式解析（ParsePrefixExpr）不消费 `~`，
// 报 parse_expected_expression "expected expression after '=', found '~'"。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_expression。
func bitwiseNot(a: Int64): Int64 {
    let x = ~a
    return x
}
