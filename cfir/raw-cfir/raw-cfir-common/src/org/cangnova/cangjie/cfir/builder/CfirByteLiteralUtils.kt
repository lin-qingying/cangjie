package org.cangnova.cangjie.cfir.builder

/**
 * 解码字节字面量（`b'x'` 语法）源文本为单个字节码点。
 *
 * 对齐官方 `LitConstKind::RUNE_BYTE`：词法层允许 `b'…'` 携带转义序列，
 * 语义层要求内容恰为一个 ≤ `UInt8.MAX` 的码点（官方 `ChkLitConstExprRange`）。
 * 不满足时返回 `null`，调用方保留字面量语法形态并交由后续诊断处理。
 */
fun byteLiteralCodePointOrNull(text: String): Int? {
    val body = text.removePrefix("b").trim()
    if (body.length < 2 || !body.startsWith('\'') || !body.endsWith('\'')) return null
    val content = body.substring(1, body.length - 1)

    val decoded = StringBuilder(content.length)
    var index = 0
    while (index < content.length) {
        val character = content[index++]
        if (character != '\\') {
            decoded.append(character)
            continue
        }
        if (index >= content.length) return null
        when (val escaped = content[index++]) {
            'b' -> decoded.append('\b')
            'f' -> decoded.append('\u000C')
            'n' -> decoded.append('\n')
            'r' -> decoded.append('\r')
            't' -> decoded.append('\t')
            '0' -> decoded.append('\u0000')
            'u' -> {
                if (index >= content.length || content[index] != '{') return null
                val closingBrace = content.indexOf('}', startIndex = index + 1)
                if (closingBrace < 0) return null
                val codePoint = content.substring(index + 1, closingBrace).toIntOrNull(16) ?: return null
                if (!Character.isValidCodePoint(codePoint)) return null
                decoded.appendCodePoint(codePoint)
                index = closingBrace + 1
            }
            else -> decoded.append(escaped)
        }
    }

    if (decoded.codePointCount(0, decoded.length) != 1) return null
    val codePoint = decoded.codePointAt(0)
    return codePoint.takeIf { it <= UByte.MAX_VALUE.toInt() }
}
