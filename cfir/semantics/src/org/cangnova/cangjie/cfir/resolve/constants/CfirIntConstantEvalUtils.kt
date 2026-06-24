package org.cangnova.cangjie.cfir.resolve.constants

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

/**
 * CFIR 整数字面量的共享语义解析入口。
 *
 * raw CFIR 构建阶段会保留整数字面量的源文本，后续 resolve、checker 和语义模型
 * 必须通过同一个入口解释进制、下划线与显式后缀，避免各阶段根据局部形态重复猜测。
 */
object CfirIntConstantEvalUtils {
    /** Int8 字面量允许的最小值。 */
    private val INT8_MIN = BigInteger.valueOf(Byte.MIN_VALUE.toLong())

    /** Int8 字面量允许的最大值。 */
    private val INT8_MAX = BigInteger.valueOf(Byte.MAX_VALUE.toLong())

    /** Int16 字面量允许的最小值。 */
    private val INT16_MIN = BigInteger.valueOf(Short.MIN_VALUE.toLong())

    /** Int16 字面量允许的最大值。 */
    private val INT16_MAX = BigInteger.valueOf(Short.MAX_VALUE.toLong())

    /** Int32 字面量允许的最小值。 */
    private val INT32_MIN = BigInteger.valueOf(Int.MIN_VALUE.toLong())

    /** Int32 字面量允许的最大值。 */
    private val INT32_MAX = BigInteger.valueOf(Int.MAX_VALUE.toLong())

    /** Int64 字面量允许的最小值。 */
    private val INT64_MIN = BigInteger.valueOf(Long.MIN_VALUE)

    /** Int64 字面量允许的最大值。 */
    private val INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE)

    /** UInt8 字面量允许的最大值。 */
    private val UINT8_MAX = BigInteger("255")

    /** UInt16 字面量允许的最大值。 */
    private val UINT16_MAX = BigInteger("65535")

    /** UInt32 字面量允许的最大值。 */
    private val UINT32_MAX = BigInteger("4294967295")

    /** UInt64 字面量允许的最大值。 */
    private val UINT64_MAX = BigInteger("18446744073709551615")

    /**
     * 已解析的无符号源文本整数字面量。
     *
     * @property originalText 保留后缀和下划线前的原始字面量文本。
     * @property value 按进制解析出的非负整数值。
     * @property explicitSuffix 显式整数后缀；为空表示源码未写后缀。
     */
    data class ParsedIntLiteral(
        val originalText: String,
        val value: BigInteger,
        val explicitSuffix: String?,
    )

    /**
     * 已解析的带符号整型常量表达式。
     *
     * @property originalText 包含一元正负号的表达式文本。
     * @property value 应用一元正负号后的整数值。
     * @property explicitSuffix 原始字面量上的显式整数后缀。
     */
    data class ParsedSignedIntExpression(
        val originalText: String,
        val value: BigInteger,
        val explicitSuffix: String?,
    )

    /**
     * 整数字面量合法取值范围。
     *
     * @property min 范围下界，闭区间。
     * @property max 范围上界，闭区间。
     */
    data class IntegerRange(
        val min: BigInteger,
        val max: BigInteger,
    ) {
        /**
         * 判断 [value] 是否落在当前闭区间内。
         */
        fun contains(value: BigInteger): Boolean = value >= min && value <= max
    }

    /**
     * 从 CFIR 字面量表达式解析整数字面量。
     *
     * 非整数字面量返回 `null`，调用方据此保持原有诊断路径。
     */
    fun parseIntLiteral(expression: CfirLiteralExpression): ParsedIntLiteral? {
        if (expression.kind != CfirLiteralKind.INT) return null
        return parseIntLiteralValue(expression.value)
    }

    /**
     * 从构建阶段保存的值对象解析整数字面量。
     *
     * 支持源文本字符串和已经被前端表示为 JVM 整数/无符号整数的值。
     */
    fun parseIntLiteralValue(value: Any?): ParsedIntLiteral? {
        return when (value) {
            is String -> parseIntLiteral(value)
            is Byte -> ParsedIntLiteral(value.toString(), BigInteger.valueOf(value.toLong()), null)
            is Short -> ParsedIntLiteral(value.toString(), BigInteger.valueOf(value.toLong()), null)
            is Int -> ParsedIntLiteral(value.toString(), BigInteger.valueOf(value.toLong()), null)
            is Long -> ParsedIntLiteral(value.toString(), BigInteger.valueOf(value), null)
            is UByte -> ParsedIntLiteral(value.toString(), BigInteger.valueOf(value.toLong()), null)
            is UShort -> ParsedIntLiteral(value.toString(), BigInteger.valueOf(value.toLong()), null)
            is UInt -> ParsedIntLiteral(value.toString(), BigInteger.valueOf(value.toLong()), null)
            is ULong -> ParsedIntLiteral(value.toString(), BigInteger(value.toString()), null)
            else -> null
        }
    }

    /**
     * 从源文本解析整数字面量。
     *
     * 支持二进制、八进制、十六进制前缀，下划线分隔符，以及 i/u 系列显式后缀。
     */
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
     * 解析 `VArray<T, $N>` 的长度字面量。
     *
     * 语法层保留的文本包含 `$` 前缀；官方语义随后按整数字面量求值到 Int64。
     * 这里复用普通整数字面量入口，统一支持进制前缀和下划线分隔符。
     */
    fun parseVArraySizeLiteral(text: String): Long? {
        val literalText = text.trim().removePrefix("$")
        val parsed = parseIntLiteral(literalText) ?: return null
        if (parsed.value < BigInteger.ZERO || parsed.value > INT64_MAX) return null
        return parsed.value.toLong()
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
            return ParsedSignedIntExpression(parsed.originalText, parsed.value, parsed.explicitSuffix)
        }

        val unaryCall = expression as? CfirFunctionCall ?: return null
        if (unaryCall.argumentList.arguments.isNotEmpty()) return null
        val receiver = unaryCall.explicitReceiver as? CfirLiteralExpression ?: return null
        val parsedReceiver = parseIntLiteral(receiver) ?: return null

        return when (extractOperatorName(unaryCall)) {
            OperatorNameConventions.UNARY_MINUS ->
                ParsedSignedIntExpression(
                    "-${parsedReceiver.originalText}",
                    parsedReceiver.value.negate(),
                    parsedReceiver.explicitSuffix,
                )

            OperatorNameConventions.UNARY_PLUS ->
                ParsedSignedIntExpression(
                    "+${parsedReceiver.originalText}",
                    parsedReceiver.value,
                    parsedReceiver.explicitSuffix,
                )

            else -> null
        }
    }

    /**
     * 根据显式整数后缀取得合法取值范围。
     *
     * @return 后缀对应的整数范围；没有后缀或后缀不属于整数类型时返回 `null`。
     */
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

    /**
     * 根据显式整数后缀取得目标 primitive cone 类型。
     *
     * @return 后缀对应的 primitive 类型；没有后缀或后缀不属于整数类型时返回 `null`。
     */
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

    /**
     * 根据上下文目标类型取得整数字面量范围。
     *
     * 当目标类型为空时，普通整数字面量默认按 Int64 范围判断；当目标类型不是 primitive
     * 整数类型时返回 `null`，由调用方选择后续错误路径。
     */
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

    /**
     * 正整数字面量没有显式期望类型时，官方语义允许它落到 UInt64 范围内。
     *
     * 这与带符号字面量不同：`9223372036854775808` 可以作为 UInt64 参与后续解析，
     * 但 `-9223372036854775809` 仍按默认 Int64 下界报越界。
     */
    fun rangeForPositiveLiteralTargetType(type: ConeCangJieType?): IntegerRange? {
        val primitive = type as? ConePrimitiveType ?: return IntegerRange(BigInteger.ZERO, UINT64_MAX)
        if (primitive.kind == PrimitiveTypeKind.IDEAL_INT) {
            return IntegerRange(BigInteger.ZERO, UINT64_MAX)
        }
        return rangeForLiteralTargetType(type)
    }

    /**
     * 带符号整数字面量没有显式期望类型时，默认仍按 Int64 范围判断。
     */
    fun rangeForSignedLiteralTargetType(type: ConeCangJieType?): IntegerRange? {
        val primitive = type as? ConePrimitiveType ?: return IntegerRange(INT64_MIN, INT64_MAX)
        if (primitive.kind == PrimitiveTypeKind.IDEAL_INT) {
            return IntegerRange(INT64_MIN, INT64_MAX)
        }
        return rangeForLiteralTargetType(type)
    }

    /**
     * 取得整数类型的位宽。
     *
     * 目标类型为空时按默认 64 位整数处理；非整数 primitive 返回 `null`。
     */
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

    /**
     * 从一元调用表达式中提取运算符名称。
     */
    private fun extractOperatorName(expression: CfirFunctionCall): Name? {
        val reference = expression.calleeReference
        return when (reference) {
            is CfirResolvedNamedReference -> reference.name
            is CfirNamedReference -> reference.name
            else -> null
        }
    }
}
