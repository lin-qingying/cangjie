// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：`@IfAvailable` 用方括号属性形式 `@IfAvailable[...]`（官方只有圆括号表达式形式 `@IfAvailable(name: literal, { lambda }, { lambda })`，见
// cangjie-features/ifAvailableExpression.cj）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_character "expected '(', found '['"。
@IfAvailable[level: "20", { 1 }, { 2 }]
func ifAvailableBracketForm(): Int64 {
    return 1
}
