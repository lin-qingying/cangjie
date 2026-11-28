// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：函数类型的形参写默认值 `(x: Int64 = 1)`（官方 ParseType.cpp ParseTypeParameterInTupleType 不解析默认值，
// 报 parse_expected_right_delimiter unclosed delimiter '(' 后接 parse_expected_dot_lparen "expected ',' or ')', found '='"，
// 再因函数体破损补 parse_missing_body；默认值只属于函数声明的具名形参 `func f(x!: Int64 = 1)`）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_right_delimiter / parse_expected_dot_lparen / parse_missing_body。
func withDefault(g: (x: Int64 = 1) -> Bool): Bool {
    return g(1)
}
