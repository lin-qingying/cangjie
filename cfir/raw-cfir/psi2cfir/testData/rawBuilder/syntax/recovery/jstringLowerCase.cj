// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：J 字符串字面量用小写 `j` 前缀（官方 Lexer.cpp 只认大写 `J` + `"`，小写 `j` 被当标识符，`j` 与 `"` 之间没有表达式）。
// 合法形态与 JString 类型见 jstringUpperCase.cj。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_character "expected ';' or '<NL>', found ''"。
func jstringLowerCase(): Unit {
    let a = j"lower"
    let _ = a
}
