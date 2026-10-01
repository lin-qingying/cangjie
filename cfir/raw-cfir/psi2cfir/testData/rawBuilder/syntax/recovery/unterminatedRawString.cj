// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：原始字符串未闭合（词法阶段）。
// 官方 cjc 1.0.5 / 1.1.3：lex_unterminated_raw_string "unterminated raw string"。
func unterminatedRawString(): String {
    return #"unterminated
}
