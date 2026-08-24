package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.isRune
import org.cangnova.cangjie.source.text

/**
 * 官方"单字符 String 字面量按目标类型改写"规则的共享判定。
 *
 * 对齐官方 `SynchronizeTypeAndInitializer` 与 `ChkAssignExpr` 的两个消费点分支：
 *  - 目标 `Rune` + `IsSingleRuneStringLiteral` → 改写为 Rune；
 *  - 目标 `UInt8`（含 `Byte` 别名展开）+ `IsSingleByteStringLiteral`（码点 ≤ 0xFF）→ 改写为 UInt8。
 *
 * 仅适用于声明初始化与普通赋值上下文；函数实参等其他 expected-type 位置官方不转换，
 * 调用方负责保证上下文（见 [CfirDeclarationsResolveTransformer] 与赋值转换入口）。
 */
internal fun CfirExpression.singleQuoteStringConversionTargetOrNull(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    val literal = this as? CfirLiteralExpression ?: return null
    if (literal.kind != CfirLiteralKind.STRING) return null
    val target = expectedType.fullyExpandedType(session)
    return when {
        target.isRune -> ConePrimitiveType.RUNE.takeIf { literal.isSingleRuneStringLiteral() }
        target is ConePrimitiveType && target.kind == PrimitiveTypeKind.UINT8 ->
            ConePrimitiveType.UINT8.takeIf { literal.isSingleByteStringLiteral() }
        else -> null
    }
}

/**
 * 声明初始化/赋值消费点上的字面量类型改写入口。
 *
 * 官方在检查初始化器/赋值右侧前直接改写字面量类型，因此该入口必须在 mismatch
 * 记录之前执行（声明侧由调用点顺序保证，赋值侧由 expected-type 驱动的字面量解析保证）。
 */
internal fun CfirExpression.applySingleQuoteStringConversion(
    expectedType: ConeCangJieType?,
    session: CfirSession,
) {
    if (expectedType == null) return
    val target = singleQuoteStringConversionTargetOrNull(expectedType, session) ?: return
    replaceConeTypeOrNull(target)
}

/** 当前 String 字面量解码后是否恰好一个 Unicode code point，是则返回该码点。 */
internal fun CfirLiteralExpression.singleCodePointOrNull(): Int? {
    if (kind != CfirLiteralKind.STRING) return null
    val value = decodedStringLiteralValueOrNull() ?: return null
    if (value.codePointCount(0, value.length) != 1) return null
    return value.codePointAt(0)
}

/** 当前无插值 String 字面量是否恰好包含一个 Unicode code point。 */
internal fun CfirLiteralExpression.isSingleRuneStringLiteral(): Boolean = singleCodePointOrNull() != null

/** 当前无插值 String 字面量是否恰好包含一个可表示为 UInt8 的码点。 */
internal fun CfirLiteralExpression.isSingleByteStringLiteral(): Boolean =
    singleCodePointOrNull()?.let { it <= UByte.MAX_VALUE.toInt() } == true

/**
 * 取得 String 字面量的语义内容，覆盖普通转义、`\u{...}` Unicode 转义与多行字符串。
 *
 * 多行字符串（`'''…'''` / `"""…"""`）按官方词法规则剥离起始换行后再参与单码点判定；
 * `#` 拼接字符串保持逐字内容。
 */
private fun CfirLiteralExpression.decodedStringLiteralValueOrNull(): String? {
    val value = value as? String ?: return null
    val sourceText = source?.text?.toString()?.trimStart().orEmpty()
    val isTripleQuoted = sourceText.startsWith("\"\"\"") || sourceText.startsWith("'''")
    val isHashQuoted = sourceText.startsWith("#")
    if (isHashQuoted || isTripleQuoted) {
        return if (isTripleQuoted) value.stripLeadingNewline() else value
    }
    if ('\\' !in value) return value

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

/** 剥离多行字符串起始处的单个换行（官方多行字符串词法语义）。 */
private fun String.stripLeadingNewline(): String = when {
    startsWith("\r\n") -> substring(2)
    startsWith('\n') || startsWith('\r') -> substring(1)
    else -> this
}
