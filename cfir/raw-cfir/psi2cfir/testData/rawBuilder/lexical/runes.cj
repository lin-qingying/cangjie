// RUN_PIPELINE_TILL: FRONTEND
// Rune 字面量：单引号 / 双引号、转义字符（反斜杠、换行、制表、单引号）、通用字符 @BS@u{...}（1~8 位十六进制）。
// 官方依据：Cangjie 文档《字符类型》「字符类型字面量」；cjc 1.0.5 / 1.1.3 零诊断。
func runeLiterals(): Unit {
    let plain = r'a'
    let plainDouble = r"b"
    let slash = r'\\'
    let newline = r'\n'
    let tab = r'\t'
    let quoteSingle = r'\''
    let quoteDouble = r"\""
    let unicodeShort = r'\u{78}'
    let unicodeCjk = r'\u{4f60}'
    let unicodeEmoji = r'\u{1F600}'
    let _ = (plain, plainDouble, slash, newline, tab, quoteSingle, quoteDouble, unicodeShort, unicodeCjk, unicodeEmoji)
}
