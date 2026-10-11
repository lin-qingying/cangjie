// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：表达式中括号未闭合。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_right_delimiter "unclosed delimiter: '('"。
func unclosedParen(): Int64 {
    return (1 + 2
}
