package org.cangnova.cangjie.cfir

/**
 * 把源码字符串字面量转换成官方 CFIR/CJO 使用的常量值。
 *
 * 官方解析器在生成 LiteralConstant 前剥除分隔符、处理多行字符串的起始换行，再执行
 * `StringConvertor::Normalize`。匹配和 CJO 写入必须共用这一转换，避免源码与二进制值失配。
 */
object CfirStringLiteralValueNormalizer {
    /** 返回合法字符串字面量的规范值；无法识别的源码形态返回 null。 */
    fun normalizeSourceLiteral(sourceText: String): String? {
        val quoteOffset = sourceText.indexOfFirst { it == '"' || it == '\'' }
        if (quoteOffset < 0) return null

        val prefix = sourceText.substring(0, quoteOffset)
        val isJString = prefix == "J"
        val delimiterHashCount = prefix.count { it == '#' }
        if (!isJString && prefix.any { it != '#' }) return null

        val quote = sourceText[quoteOffset]
        val quoteWidth = if (delimiterHashCount == 0 &&
            sourceText.startsWith(quote.toString().repeat(3), quoteOffset)
        ) {
            3
        } else {
            1
        }
        val delimiter = quote.toString().repeat(quoteWidth)
        val closingOffset = sourceText.lastIndexOf(delimiter)
        if (closingOffset < quoteOffset + quoteWidth) return null

        val suffix = sourceText.substring(closingOffset + quoteWidth)
        if (suffix.length != delimiterHashCount || suffix.any { it != '#' }) return null

        val isRaw = delimiterHashCount > 0
        var content = sourceText.substring(quoteOffset + quoteWidth, closingOffset)
        if (!isRaw && quoteWidth == 3) {
            content = when {
                content.startsWith("\r\n") -> content.substring(2)
                content.startsWith('\r') || content.startsWith('\n') -> content.substring(1)
                else -> return null
            }
        }
        return normalizeContent(content, isRaw, allowDollarEscape = true)
    }

    /** 返回 Rune 字面量的 Unicode 标量值；语法形态或转义无效时返回 null。 */
    fun normalizeRuneSourceLiteral(sourceText: String): String? {
        if (sourceText.length < 4 || sourceText.first() != 'r') return null
        val quote = sourceText[1]
        if ((quote != '\'' && quote != '"') || sourceText.last() != quote) return null
        val normalized = normalizeContent(
            sourceText.substring(2, sourceText.lastIndex),
            isRaw = false,
            allowDollarEscape = true,
        ) ?: return null
        return normalized.takeIf { it.codePointCount(0, it.length) == 1 }
    }

    /** 对齐官方 `StringConvertor::Normalize` 的转义表及 CR/CRLF 归一化。 */
    private fun normalizeContent(
        content: String,
        isRaw: Boolean,
        allowDollarEscape: Boolean,
    ): String? {
        val normalized = StringBuilder(content.length)
        var index = 0
        while (index < content.length) {
            val character = content[index++]
            when {
                character == '\\' && !isRaw -> {
                    if (index >= content.length) return null
                    when (content[index++]) {
                        'u' -> {
                            if (index >= content.length || content[index] != '{') return null
                            val closingBrace = content.indexOf('}', startIndex = index + 1)
                            if (closingBrace < 0) return null
                            val codePoint = content.substring(index + 1, closingBrace).toIntOrNull(16) ?: return null
                            if (!Character.isValidCodePoint(codePoint) || codePoint in 0xD800..0xDFFF) return null
                            normalized.appendCodePoint(codePoint)
                            index = closingBrace + 1
                        }

                        '0' -> normalized.append('\u0000')
                        '\\' -> normalized.append('\\')
                        'b' -> normalized.append('\b')
                        'f' -> normalized.append('\u000C')
                        'n' -> normalized.append('\n')
                        'r' -> normalized.append('\r')
                        't' -> normalized.append('\t')
                        'v' -> normalized.append('\u000B')
                        '\'' -> normalized.append('\'')
                        '"' -> normalized.append('"')
                        '?' -> normalized.append('?')
                        '$' -> if (allowDollarEscape) normalized.append('$') else return null
                        else -> return null
                    }
                }

                character == '\r' -> {
                    if (index < content.length && content[index] == '\n') index++
                    normalized.append('\n')
                }

                else -> normalized.append(character)
            }
        }
        return normalized.toString()
    }
}
