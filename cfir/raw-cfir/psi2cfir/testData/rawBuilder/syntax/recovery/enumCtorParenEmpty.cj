// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：枚举构造器写空参数列表 `| B()`（官方 parse_expected_type "expected type name after '(', found ')'"，锚 `)`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_type。
enum EmptyCtor {
    | A
    | B()
}
