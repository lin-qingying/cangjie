// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：把 `with` 当表达式（`with(r) { x => ... }`）。官方 1.0.5 / 1.1.3 没有 with 表达式：作为语句报 parse_expected_expr_or_decl_in "expected expression or
// declaration, found keyword 'with'"，作为右值报 parse_expected_expression "expected expression after keyword 'return', found keyword 'with'"。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_expr_or_decl_in / parse_expected_expression。
class R {
    public init() {}

    public func use(): Int64 {
        return 1
    }
}

func withExpression(r: R): Int64 {
    with(r) { x => x.use() }
    return with(r) { x => x.use() }
}
