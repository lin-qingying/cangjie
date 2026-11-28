// RUN_PIPELINE_TILL: FRONTEND
// 多行原始字符串字面量：`#` + 引号后必须紧跟**真实换行**（与多行字符串一致），井号个数决定结束标记，
// 井号少于结束标记的引号序列只是普通字符；原始字符串里 `\` 按原样保留（不构成转义）。
// 空的多行原始字符串（`#""""""#`）被官方 Lexer.cpp ScanMultiLineRawString 接受（不要求换行），而空的多行字符串不行，见 recovery。
// 官方依据：Cangjie 文档《字符串类型》「多行原始字符串字面量」；官方 Lexer.cpp ScanMultiLineRawString；cjc 1.0.5 / 1.1.3 零诊断。
func rawStringLiterals(): Unit {
    let emptyHash = #""""""#
    let emptyHashSingle = #''''''#
    let emptyHashTwoHash = ##""""""##
    let singleHash = #"
raw
stays
"#
    let singleHashSingle = #'
raw
stays
'#
    let twoHash = ##"a "# inside"##
    let twoHashSingle = ##'a '# inside'##
    let threeHash = ###""""""
        multi
        line "## inside"###
    let hashInside = #"
a # b
"#
    let backslash = #"
C:\\temp\\x
"#
    let _ = (emptyHash, emptyHashSingle, emptyHashTwoHash, singleHash, singleHashSingle, twoHash, twoHashSingle, threeHash, hashInside, backslash)
}
