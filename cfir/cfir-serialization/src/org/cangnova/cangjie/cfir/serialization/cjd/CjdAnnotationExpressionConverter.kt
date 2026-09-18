package org.cangnova.cangjie.cfir.serialization.cjd

import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.name.Name
import java.math.BigInteger

/** 仅解码 parser 已确认的字面量；运算、调用与插值保留为带原文诊断的错误表达式。 */
internal class CjdAnnotationExpressionConverter(
    private val sourceId: String,
    private val diagnostics: MutableList<CjdAnnotationConversionDiagnostic>,
) {
    fun convert(syntax: CjdAnnotationExpression, parserOwnedReference: Boolean = false): CfirExpression {
        if (parserOwnedReference) {
            val token = if (syntax.syntaxKind == "IDENTIFIER") syntax else syntax.children.singleOrNull()
            if (token?.syntaxKind == "IDENTIFIER") {
                val referenceName = Name.identifierIfValid(token.rawText.removeSurrounding("`"))
                if (referenceName != null) return buildNamedAccessExpression {
                    source = cjdAnnotationSource(syntax.rawText, syntax.range, sourceId)
                    calleeReference = buildNamedReference { name = referenceName }
                }
            }
        }
        fun literal(kind: CfirLiteralKind, value: Any?): CfirExpression = buildLiteralExpression {
            source = cjdAnnotationSource(syntax.rawText, syntax.range, sourceId)
            this.kind = kind
            this.value = value
        }
        val raw = syntax.rawText.trim()
        return when (syntax.syntaxKind) {
            "STRING_TEMPLATE" -> stringValue(syntax)?.let { literal(CfirLiteralKind.STRING, it) } ?: error(syntax)
            "BOOLEAN_CONSTANT" -> when (raw) {
                "true" -> literal(CfirLiteralKind.BOOLEAN, true)
                "false" -> literal(CfirLiteralKind.BOOLEAN, false)
                else -> error(syntax)
            }
            "INTEGER_CONSTANT" -> integer(raw)?.let { literal(CfirLiteralKind.INT, it) } ?: error(syntax)
            "FLOAT_CONSTANT" -> raw.replace("_", "").removeSuffix("f16").removeSuffix("f32").removeSuffix("f64")
                .toDoubleOrNull()?.let { literal(CfirLiteralKind.FLOAT, it) } ?: error(syntax)
            "UNIT_CONSTANT" -> literal(CfirLiteralKind.UNIT, Unit)
            "PARENTHESIZED" -> syntax.children.filterNot { it.rawText == "(" || it.rawText == ")" }.singleOrNull()?.let { convert(it) } ?: error(syntax)
            else -> error(syntax)
        }
    }

    private fun stringValue(syntax: CjdAnnotationExpression): String? {
        if (syntax.children.firstOrNull()?.syntaxKind != "OPEN_QUOTE" || syntax.children.lastOrNull()?.syntaxKind != "CLOSING_QUOTE") return null
        val content = StringBuilder()
        for (entry in syntax.children.drop(1).dropLast(1)) {
            when (entry.syntaxKind) {
                "LITERAL_STRING_TEMPLATE_ENTRY" -> content.append(entry.rawText)
                "ESCAPE_STRING_TEMPLATE_ENTRY" -> {
                    // 与 PSI escape entry 共用 IntelliJ 解码器；仓颉 Unicode 标量形式单独处理。
                    val text = entry.rawText
                    if (text.startsWith("\\u{") && text.endsWith("}")) {
                        val point = text.substring(3, text.length - 1).toIntOrNull(16) ?: return null
                        if (!Character.isValidCodePoint(point) || point in 0xD800..0xDFFF) return null
                        content.appendCodePoint(point)
                    } else content.append(StringUtil.unescapeStringCharacters(text))
                }
                else -> return null
            }
        }
        return content.toString()
    }

    private fun integer(raw: String): BigInteger? {
        var value = raw.replace("_", "")
        val suffix = listOf("i8", "i16", "i32", "i64", "u8", "u16", "u32", "u64", "iNative", "uNative").firstOrNull(value::endsWith)
        if (suffix != null) value = value.removeSuffix(suffix)
        val radix = when {
            value.startsWith("0x", true) -> 16
            value.startsWith("0b", true) -> 2
            value.startsWith("0o", true) -> 8
            else -> 10
        }
        return (if (radix == 10) value else value.drop(2)).toBigIntegerOrNull(radix)
    }

    private fun error(syntax: CjdAnnotationExpression): CfirErrorExpression {
        val diagnostic = CjdAnnotationConversionDiagnostic(CjdAnnotationDiagnosticKind.UNSUPPORTED_EXPRESSION,
            sourceId, syntax.range, syntax.rawText, "Unsupported sidecar annotation expression: ${syntax.syntaxKind}: ${syntax.rawText}")
        diagnostics += diagnostic
        return buildErrorExpression {
            source = cjdAnnotationSource(syntax.rawText, syntax.range, sourceId)
            this.diagnostic = diagnostic
        }
    }
}
