// 字符串与字符字面量：普通字符串、单引号字符串、三引号原始字符串、哈希字符串、
// 字符串插值、rune 字符字面量与字节字符字面量。
// 官方依据：Cangjie 文档《字符串类型》《字符类型》；cjc 1.0.5 / 1.1.3 零诊断。
func stringAndRuneLiterals(): Unit {
    let name = "cj"
    let a = 1
    let b = 2
    let doubleQuoted = "hello"
    let singleQuoted = 'world'
    let multiline = """
        line one
        line two
    """
    let multilineSingle = '''
        raw single
    '''
    let hashString = #"hash # quoted"#
    let hashMulti = ##"hash ## quoted"##
    let templateShort = "value: $name"
    let templateLong = "sum: ${a + b}"
    let escaped = "tab\t newline\n quote\" backslash\\"
    let unicode = "\u{1F600}"
    let rune = r'a'
    let runeDouble = r"b"
    let runeEscape = r'\n'
    let byte = b'x'
    let byteEscape = b'\n'
    let byteUnicode = b'\u{41}'
    let _ = (doubleQuoted, singleQuoted, multiline, multilineSingle)
    let _ = (hashString, hashMulti, templateShort, templateLong)
    let _ = (escaped, unicode, rune, runeDouble, runeEscape)
    let _ = (byte, byteEscape, byteUnicode)
}
