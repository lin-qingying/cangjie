// 转义序列：字节 / rune / 字符串中的 \0、\u{...}、\\、\'、\" 等全部转义形态。
// 官方依据：Cangjie 文档《字符类型》《字符串类型》；cjc 1.0.5 / 1.1.3 零诊断。
func escapeForms(): Unit {
    let b0 = b'\0'
    let b1 = b'\u{0}'
    let b2 = b' '
    let r0 = r'\u{0}'
    let r1 = r'\\'
    let r2 = r'\''
    let r3 = r"\""
    let s0 = "\\u{41}"
    let s1 = "a\"b"
    let s2 = '\''
    let _ = (b0, b1, b2, r0, r1, r2, r3, s0, s1, s2)
}
