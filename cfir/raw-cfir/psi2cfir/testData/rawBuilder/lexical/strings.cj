// RUN_PIPELINE_TILL: FRONTEND
// 单行字符串字面量：双引号 / 单引号、空串、转义字符（引号、反斜杠、换行、制表、回车、退格、换页、垂直制表、NUL、通用字符）。
// 官方依据：Cangjie 文档《字符串类型》「单行字符串字面量」；cjc 1.0.5 / 1.1.3 零诊断。
func stringLiterals(): Unit {
    let empty = ""
    let emptySingle = ''
    let plain = "hello"
    let plainSingle = 'hello'
    let withSpace = "hello world"
    let quoteDouble = "say \"hi\""
    let quoteSingle = 'say \'hi\''
    let backslash = "a\\b"
    let escapes = "n\nt\tr\rb\bf\fv\v0\0"
    let unicode = "u\u{4f60}\u{597d}"
    let chinese = "仓颉"
    let emoji = "😀"
    let _ = (empty, emptySingle, plain, plainSingle, withSpace, quoteDouble, quoteSingle, backslash, escapes, unicode, chinese, emoji)
}
