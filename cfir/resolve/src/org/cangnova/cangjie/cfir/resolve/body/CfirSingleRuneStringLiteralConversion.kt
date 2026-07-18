package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.isRune
import org.cangnova.cangjie.source.text

/**
 * 对齐官方 `IsSingleRuneStringLiteral` 及变量初始化/赋值检查：单字符 String 字面量在
 * Rune 声明初始化和普通赋值上下文中改写为 Rune。函数实参等其他 expected-type 上下文
 * 不使用该转换，官方会继续报告 String 到 Rune 的类型不匹配。
 */
internal fun CfirExpression.applySingleRuneStringLiteralConversion(expectedType: ConeCangJieType?) {
    if (expectedType?.isRune != true) return
    val literal = this as? CfirLiteralExpression ?: return
    if (literal.kind != CfirLiteralKind.STRING) return

    if (!literal.isSingleRuneStringLiteral()) return
    literal.replaceConeTypeOrNull(ConePrimitiveType.RUNE)
}

/** 当前无插值 String 字面量是否恰好包含一个 Unicode code point。 */
internal fun CfirLiteralExpression.isSingleRuneStringLiteral(): Boolean {
    if (kind != CfirLiteralKind.STRING) return false
    val value = decodedStringLiteralValueOrNull() ?: return false
    return value.codePointCount(0, value.length) == 1
}

/** 取得无插值 String 字面量的语义内容，覆盖普通转义与 `\\u{...}` Unicode 转义。 */
private fun CfirLiteralExpression.decodedStringLiteralValueOrNull(): String? {
    val value = value as? String ?: return null
    val sourceText = source?.text?.toString()?.trimStart().orEmpty()
    val isRawLiteral = sourceText.startsWith("#") ||
            sourceText.startsWith("\"\"\"") ||
            sourceText.startsWith("'''")
    if (isRawLiteral || '\\' !in value) return value

    val result = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index++]
        if (character != '\\') {
            result.append(character)
            continue
        }
        if (index >= value.length) return null

        when (val escaped = value[index++]) {
            'b' -> result.append('\b')
            'f' -> result.append('\u000C')
            'n' -> result.append('\n')
            'r' -> result.append('\r')
            't' -> result.append('\t')
            'u' -> {
                if (index >= value.length || value[index] != '{') {
                    result.append('u')
                    continue
                }
                val closingBrace = value.indexOf('}', startIndex = index + 1)
                if (closingBrace < 0) return null
                val codePoint = value.substring(index + 1, closingBrace).toIntOrNull(16) ?: return null
                if (!Character.isValidCodePoint(codePoint)) return null
                result.appendCodePoint(codePoint)
                index = closingBrace + 1
            }
            else -> result.append(escaped)
        }
    }
    return result.toString()
}
