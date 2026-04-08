package org.cangnova.cangjie.cfir.analysis.checkers.expression

import java.math.BigInteger
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

internal object CfirIntConstantEvalUtils {
    private val INT8_MIN = BigInteger.valueOf(Byte.MIN_VALUE.toLong())
    private val INT8_MAX = BigInteger.valueOf(Byte.MAX_VALUE.toLong())
    private val INT16_MIN = BigInteger.valueOf(Short.MIN_VALUE.toLong())
    private val INT16_MAX = BigInteger.valueOf(Short.MAX_VALUE.toLong())
    private val INT32_MIN = BigInteger.valueOf(Int.MIN_VALUE.toLong())
    private val INT32_MAX = BigInteger.valueOf(Int.MAX_VALUE.toLong())
    private val INT64_MIN = BigInteger.valueOf(Long.MIN_VALUE)
    private val INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE)

    private val UINT8_MAX = BigInteger("255")
    private val UINT16_MAX = BigInteger("65535")
    private val UINT32_MAX = BigInteger("4294967295")
    private val UINT64_MAX = BigInteger("18446744073709551615")

    data class ParsedIntLiteral(
        val originalText: String,
        val value: BigInteger,
        val explicitSuffix: String?,
    )

    data class ParsedSignedIntExpression(
        val value: BigInteger,
        val explicitSuffix: String?,
    )

    data class IntegerRange(
        val min: BigInteger,
        val max: BigInteger,
    ) {
        fun contains(value: BigInteger): Boolean = value >= min && value <= max
    }

    fun parseIntLiteral(expression: CfirLiteralExpression): ParsedIntLiteral? {
        if (expression.kind != CfirLiteralKind.INT) return null
        val text = expression.value as? String ?: return null
        return parseIntLiteral(text)
    }

    fun parseIntLiteral(text: String): ParsedIntLiteral? {
        val raw = text.trim()
        if (raw.isEmpty()) return null

        val suffix = Regex("(?i)(u8|u16|u32|u64|i8|i16|i32|i64)$")
            .find(raw)
            ?.groupValues
            ?.get(1)
        val core = if (suffix != null) raw.dropLast(suffix.length) else raw
        val normalized = core.replace("_", "")
        if (normalized.isEmpty()) return null

        val (radix, digits) = when {
            normalized.startsWith("0x", ignoreCase = true) -> 16 to normalized.substring(2)
            normalized.startsWith("0b", ignoreCase = true) -> 2 to normalized.substring(2)
            normalized.startsWith("0o", ignoreCase = true) -> 8 to normalized.substring(2)
            else -> 10 to normalized
        }
        if (digits.isEmpty()) return null

        return try {
            ParsedIntLiteral(
                originalText = raw,
                value = BigInteger(digits, radix),
                explicitSuffix = suffix?.lowercase(),
            )
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * 提取“带符号”的整型常量表达式。
     *
     * 这里显式支持 `1`、`+1`、`-1` 三类稳定形态。
     * 对于 `1 << -1` 这样的表达式，右操作数在 CFIR 中会被编码成
     * `UNARY_MINUS(literal)` 的 operator call，因此不能只看字面量节点本身。
     */
    fun parseSignedIntExpression(expression: CfirExpression): ParsedSignedIntExpression? {
        val literal = expression as? CfirLiteralExpression
        if (literal != null) {
            val parsed = parseIntLiteral(literal) ?: return null
            return ParsedSignedIntExpression(parsed.value, parsed.explicitSuffix)
        }

        val unaryCall = expression as? CfirFunctionCall ?: return null
        if (unaryCall.argumentList.arguments.isNotEmpty()) return null
        val receiver = unaryCall.explicitReceiver as? CfirLiteralExpression ?: return null
        val parsedReceiver = parseIntLiteral(receiver) ?: return null

        return when (extractOperatorName(unaryCall)) {
            OperatorNameConventions.UNARY_MINUS ->
                ParsedSignedIntExpression(parsedReceiver.value.negate(), parsedReceiver.explicitSuffix)

            OperatorNameConventions.UNARY_PLUS ->
                ParsedSignedIntExpression(parsedReceiver.value, parsedReceiver.explicitSuffix)

            else -> null
        }
    }

    fun rangeForExplicitSuffix(suffix: String?): IntegerRange? {
        return when (suffix?.lowercase()) {
            "i8" -> IntegerRange(INT8_MIN, INT8_MAX)
            "i16" -> IntegerRange(INT16_MIN, INT16_MAX)
            "i32" -> IntegerRange(INT32_MIN, INT32_MAX)
            "i64" -> IntegerRange(INT64_MIN, INT64_MAX)
            "u8" -> IntegerRange(BigInteger.ZERO, UINT8_MAX)
            "u16" -> IntegerRange(BigInteger.ZERO, UINT16_MAX)
            "u32" -> IntegerRange(BigInteger.ZERO, UINT32_MAX)
            "u64" -> IntegerRange(BigInteger.ZERO, UINT64_MAX)
            else -> null
        }
    }

    fun coneTypeForExplicitSuffix(suffix: String?): ConePrimitiveType? {
        return when (suffix?.lowercase()) {
            "i8" -> ConePrimitiveType.INT8
            "i16" -> ConePrimitiveType.INT16
            "i32" -> ConePrimitiveType.INT32
            "i64" -> ConePrimitiveType.INT64
            "u8" -> ConePrimitiveType.UINT8
            "u16" -> ConePrimitiveType.UINT16
            "u32" -> ConePrimitiveType.UINT32
            "u64" -> ConePrimitiveType.UINT64
            else -> null
        }
    }

    fun rangeForLiteralTargetType(type: ConeCangJieType?): IntegerRange? {
        val primitive = type as? ConePrimitiveType ?: return IntegerRange(INT64_MIN, INT64_MAX)
        return when (primitive.kind) {
            PrimitiveTypeKind.INT8 -> IntegerRange(INT8_MIN, INT8_MAX)
            PrimitiveTypeKind.INT16 -> IntegerRange(INT16_MIN, INT16_MAX)
            PrimitiveTypeKind.INT32 -> IntegerRange(INT32_MIN, INT32_MAX)
            PrimitiveTypeKind.INT64,
            PrimitiveTypeKind.INT_NATIVE,
            PrimitiveTypeKind.IDEAL_INT -> IntegerRange(INT64_MIN, INT64_MAX)
            PrimitiveTypeKind.UINT8 -> IntegerRange(BigInteger.ZERO, UINT8_MAX)
            PrimitiveTypeKind.UINT16 -> IntegerRange(BigInteger.ZERO, UINT16_MAX)
            PrimitiveTypeKind.UINT32 -> IntegerRange(BigInteger.ZERO, UINT32_MAX)
            PrimitiveTypeKind.UINT64,
            PrimitiveTypeKind.UINT_NATIVE -> IntegerRange(BigInteger.ZERO, UINT64_MAX)
            else -> null
        }
    }

    fun bitWidthForIntegerType(type: ConeCangJieType?): Int? {
        val primitive = type as? ConePrimitiveType ?: return 64
        return when (primitive.kind) {
            PrimitiveTypeKind.INT8,
            PrimitiveTypeKind.UINT8 -> 8

            PrimitiveTypeKind.INT16,
            PrimitiveTypeKind.UINT16 -> 16

            PrimitiveTypeKind.INT32,
            PrimitiveTypeKind.UINT32 -> 32

            PrimitiveTypeKind.INT64,
            PrimitiveTypeKind.UINT64,
            PrimitiveTypeKind.INT_NATIVE,
            PrimitiveTypeKind.UINT_NATIVE,
            PrimitiveTypeKind.IDEAL_INT -> 64

            else -> null
        }
    }

    private fun extractOperatorName(expression: CfirFunctionCall): Name? {
        val reference = expression.calleeReference
        return when (reference) {
            is CfirResolvedNamedReference -> reference.name
            is CfirNamedReference -> reference.name
            else -> null
        }
    }
}
