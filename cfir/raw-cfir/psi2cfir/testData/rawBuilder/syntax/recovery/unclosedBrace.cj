// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：函数体缺少右花括号。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_right_delimiter "unclosed delimiter: '{'"。
func unclosedBrace(): Int64 {
    return 1
