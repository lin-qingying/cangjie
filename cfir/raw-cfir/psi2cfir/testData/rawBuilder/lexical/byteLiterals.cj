// RUN_PIPELINE_TILL: FRONTEND
// 字符字节字面量 b'x'：普通字符、转义字符（换行、制表、反斜杠、单引号）、通用字符（1~2 位十六进制，值 < 256）。
// 官方依据：Cangjie 文档《整数类型》「字符字节字面量」；cjc 1.0.5 / 1.1.3 零诊断。
func byteLiterals(): Unit {
    let plain = b'x'
    let digit = b'0'
    let newline = b'\n'
    let tab = b'\t'
    let slash = b'\\'
    let singleQuote = b'\''
    let unicode = b'\u{78}'
    let unicodeUpper = b'\u{90}'
    let _ = (plain, digit, newline, tab, slash, singleQuote, unicode, unicodeUpper)
}
