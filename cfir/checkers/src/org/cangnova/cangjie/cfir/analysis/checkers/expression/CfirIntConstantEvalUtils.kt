package org.cangnova.cangjie.cfir.analysis.checkers.expression

import java.math.BigInteger
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

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

    fun rangeForLiteralTargetType(type: ConeCangjieType?): IntegerRange? {
        val primitive = type as? ConePrimitiveType ?: return IntegerRange(INT64_MIN, INT64_MAX)
        return when (primitive.kind) {
            PrimitiveTypeKind.INT8 -> IntegerRange(INT8_MIN, INT8_MAX)
            PrimitiveTypeKind.INT16 -> IntegerRange(INT16_MIN, INT16_MAX)
            PrimitiveTypeKind.INT32 -> IntegerRange(INT32_MIN, INT32_MAX)
            PrimitiveTypeKind.INT64, PrimitiveTypeKind.IDEAL_INT -> IntegerRange(INT64_MIN, INT64_MAX)
            PrimitiveTypeKind.UINT8 -> IntegerRange(BigInteger.ZERO, UINT8_MAX)
            PrimitiveTypeKind.UINT16 -> IntegerRange(BigInteger.ZERO, UINT16_MAX)
            PrimitiveTypeKind.UINT32 -> IntegerRange(BigInteger.ZERO, UINT32_MAX)
            PrimitiveTypeKind.UINT64 -> IntegerRange(BigInteger.ZERO, UINT64_MAX)
            else -> null
        }
    }
}

