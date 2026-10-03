// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：空的多行字符串字面量 `""""""`（`"""` 紧接 `"""`，中间没有真实换行）——官方 Lexer.cpp ScanMultiLineString
// 要求多行字符串在三个引号之后必须紧跟行终止符，否则报 lex_multiline_string_start_from_newline（原始字符串无此要求，见 lexical/rawStrings.cj）。
// 官方 cjc 1.0.5 / 1.1.3：2 条 lex_multiline_string_start_from_newline（""" 与 ''' 各 1 条）。
func emptyMultilineStrings(): String {
    let a = """"""
    let b = ''''''
    return a + b
}
