// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：单行字符串未闭合（词法阶段）。
// 官方 cjc 1.0.5 / 1.1.3：lex_unterminated_single_line_string "unterminated single-line string"。
func unterminatedString(): String {
    return "unterminated
}
