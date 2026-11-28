package org.cangnova.cangjie.cfir.resolve.constants

import java.math.BigDecimal
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * CFIR 浮点字面量的共享语义入口。
 *
 * 对齐官方 `InitializeLitConstValue`（`external/cangjie_compiler/src/AST/Utils.cpp:192-235`）：
 * 官方把字面量文本去下划线后用 `stold` 解析，溢出会抛 `out_of_range`，此时用
 * `FloatFormat::IsUnderFlowFloat`（`src/Utils/FloatFormat.cpp:55-66`，判据是"解析后绝对值小于 1"）
 * 分流成 `FlowStatus::UNDER` 或 `FlowStatus::OVER`；正常值带 `FlowStatus::NORMAL`。
 * 范围判定在 `ChkFloatTypeOverflow`（`src/Sema/CalcConstExpr.cpp:30-85`）分两段：
 * 先看 `flowStatus`，再把值转成目标位宽比对 inf 位模式与"归零"位模式。
 *
 * 官方取值是 `long double` 高精度值加独立状态位（`include/cangjie/AST/Node.h:1622-1630`）。
 * CFIR 的 `CfirLiteralExpression.value` 载荷须满足 `CfirConstantValue.Primitive` 与 Analysis API
 * 对 `Double` 的既有约定（`CfirConstantValue.kt:28`、`CaCfirAnnotations.kt:175`），
 * 因此此处存 `Double`；官方 `value != 0` 这个下溢前置守卫所需的"精确量级是否非零"，
 * 由字面量原文经 [BigDecimal] 推导，不额外引入状态位。
 */
object CfirFloatConstantEvalUtils {
    /**
     * 官方 `GetFloatTypeInfoByKind`（`src/AST/Utils.cpp:179-191`）的浮点类型边界。
     *
     * @property min 官方诊断文案中的最小可表示值。
     * @property max 官方诊断文案中的最大可表示值。
     */
    data class FloatTypeInfo(
        val min: String,
        val max: String,
    )

    /** 浮点字面量的显式类型后缀，官方 `ProcessFloatSuffix`（`src/Lex/Lexer.cpp:465-479`）。 */
    private val FLOAT_SUFFIX_REGEX = Regex("(?i)(f16|f32|f64)$")

    /**
     * 取得目标浮点类型的官方边界信息。
     *
     * `IDEAL_FLOAT` 与 `FLOAT64` 共用同一行，与官方表一致；非浮点类型返回 `null`。
     */
    fun floatTypeInfoFor(kind: PrimitiveTypeKind): FloatTypeInfo? = when (kind) {
        PrimitiveTypeKind.FLOAT16 -> FloatTypeInfo("5.960464477539063E-8", "6.5504E4")
        PrimitiveTypeKind.FLOAT32 -> FloatTypeInfo("1.40129846E-45", "3.40282347E38")
        PrimitiveTypeKind.FLOAT64, PrimitiveTypeKind.IDEAL_FLOAT ->
            FloatTypeInfo("4.9406564584124654E-324", "1.7976931348623157E308")

        else -> null
    }

    /**
     * 从字面量源文本解析浮点值，写入 [org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression.value]。
     *
     * 与官方一致地先去下划线；`Double.parseDouble` 对超出 double 的字面量返回无穷大而不抛异常，
     * 这正好对应官方把 `FlowStatus::OVER` 的值置为 inf 位模式，溢出由值本身承载。
     * 词法器保证字面量合法，解析失败返回 `null` 交由调用方保持原有路径。
     */
    fun parseFloatLiteral(text: String): Double? {
        val canonical = text.trim().replace(FLOAT_SUFFIX_REGEX, "").replace("_", "")
        if (canonical.isEmpty()) return null
        return try {
            canonical.toDouble()
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * 判断字面量转成目标位宽后是否溢出为 inf。
     *
     * 官方比对的是转换结果的 inf 位模式（`CalcConstExpr.cpp:47-69`），不是拿字面量值与
     * 上下界比较。cjc 1.0.5 探针：`var a: Float32 = 3.4028235e38` 通过，`3.5e38` 报
     * `sema_float_literal_too_large`——前者作为 double 解析后转 float32 恰好等于
     * `Float.MAX_VALUE` 而非 inf。按上下界比较会把这条误报。
     */
    fun convertsToInfinity(value: Double, target: PrimitiveTypeKind): Boolean {
        val shifted = shiftedValueBits(value, target) ?: return false
        return shifted == infBitsOf(target)
    }

    /**
     * 判断字面量转成目标位宽后是否归零。
     *
     * 官方判据是"高精度值非零但目标位宽为零"（`value != 0 && value == 0`，round to zero），
     * 因此字面量本身就写作 0 时不报下溢。cjc 探针：`var d: Float64 = 0.0e-400` 不报，
     * 而 `var h: Float64 = 1e-400f64` 报。
     *
     * @param value 字面量解析出的数值。
     * @param target 目标浮点类型。
     * @param exactNonZero 字面量原文的精确量级是否非零，对应官方的高精度值非零判断。
     */
    fun convertsToZero(value: Double, target: PrimitiveTypeKind, exactNonZero: Boolean): Boolean {
        if (!exactNonZero) return false
        val shifted = shiftedValueBits(value, target) ?: return false
        return shifted == 0L
    }

    /**
     * 目标位宽的原始位模式左移一位，作为范围判定的唯一依据。
     *
     * 对齐官方 `ChkFloatTypeOverflow`（`src/Sema/CalcConstExpr.cpp:22,28,34`）的
     * `value = bits << 1`。移位位宽不同导致一个必须复现的官方行为：Float32/Float64 在
     * `uint32_t`/`uint64_t` 内移位会丢掉符号位，负值照常上报；Float16 的
     * `Float32ToFloat16` 返回 `uint16_t`，`<< 1` 提升为 `int`，符号位落到第 16 位，
     * 于是负的 Float16 字面量既不等于 0 也不等于 inf 位模式，一律不报
     * （cjc 1.0.5 探针：`-70000.0` / `-1e-8` / `-1e-40` 配 Float16 均静默）。
     *
     * @return 左移一位后的位模式；目标不是浮点类型时返回 `null`。
     */
    private fun shiftedValueBits(value: Double, target: PrimitiveTypeKind): Long? = when (target) {
        PrimitiveTypeKind.FLOAT16 -> (float32ToFloat16(value) shl 1).toLong() and 0xFFFFFFFFL
        // 必须先在 32 位 Int 内移位：官方 `(*reinterpret_cast<uint32_t*>(&f32)) << 1`
        // 的移位发生在 32 位字内，符号位因此被移出（该行注释即 "1: remove the sign bit"）。
        // 若先提升为 Long 再移位，符号位会停在第 32 位，负 inf 得到 0x1FF000000 而非
        // 0xFF000000，导致 `-1e39` 这类负值溢出漏报。
        PrimitiveTypeKind.FLOAT32 ->
            (java.lang.Float.floatToRawIntBits(value.toFloat()) shl 1).toLong() and 0xFFFFFFFFL
        PrimitiveTypeKind.FLOAT64, PrimitiveTypeKind.IDEAL_FLOAT ->
            java.lang.Double.doubleToRawLongBits(value) shl 1
        else -> null
    }

    /**
     * 官方 `GetFloatTypeInfoByKind` 的 inf 位模式。
     *
     * 该表存的是**左移一位之后**的位模式，不是原始 inf 位模式：正 inf 的
     * `0x7FF0000000000000 << 1` 正是 `0xFFE0000000000000`，移位的目的就是把符号位
     * 移出 64 位字，使正负 inf 归一到同一个比较值。
     * `0xFFE0000000000000` 超出有符号 Long 上限，故用 ULong 字面量再转回。
     */
    private fun infBitsOf(target: PrimitiveTypeKind): Long = when (target) {
        PrimitiveTypeKind.FLOAT16 -> 0xF800L
        PrimitiveTypeKind.FLOAT32 -> 0xFF000000L
        else -> 0xFFE0000000000000UL.toLong()
    }

    /**
     * 官方 `FloatFormat::Float32ToFloat16`（`src/Utils/FloatFormat.cpp:33-53`）的位级转换。
     *
     * 次正规分支的移位量可超过 31（指数极小时），C++ 侧是未定义行为；此处显式收敛为 0，
     * 因为这些输入的尾数本就小于 2^23，任何有效移位量下结果同样是 0。
     */
    private fun float32ToFloat16(value: Double): Int {
        val bits = java.lang.Float.floatToRawIntBits(value.toFloat())
        val exp = ((bits and FLOAT32_EXP_MASK) ushr FLOAT32_TAIL_WIDTH) - FLOAT32_EXP_BASE + FLOAT16_EXP_BASE
        val result = when {
            exp > FLOAT16_EXP_MAX -> FLOAT16_INF
            exp <= 0 -> {
                val shift = FLOAT32_FLOAT16_TAIL_OFFSET + (1 - exp)
                if (shift >= Int.SIZE_BITS) {
                    0
                } else {
                    ((bits and FLOAT32_TAIL_MASK) or (1 shl FLOAT32_TAIL_WIDTH)) ushr shift
                }
            }

            else ->
                ((bits and FLOAT32_TAIL_MASK) ushr FLOAT32_FLOAT16_TAIL_OFFSET) or (exp shl FLOAT16_TAIL_WIDTH)
        }
        return if (bits < 0) result or FLOAT16_SIGN_MASK else result and 0xFFFF
    }

    /**
     * 判断浮点字面量原文的精确量级是否非零。
     *
     * 官方用 `long double` 承载高精度值再判 `value != 0`；此处用 [BigDecimal] 读原文，
     * 精度不低于 long double，且对 `1e-400` 这类在 double 下已归零的字面量仍能判出非零。
     * 原文不可用或不是合法十进制数时返回 `true`，即不因缺文本而漏报下溢。
     */
    fun hasExactNonZeroMagnitude(text: String?): Boolean {
        val literalText = text?.trim()?.takeIf { it.isNotEmpty() } ?: return true
        val canonical = literalText.replace(FLOAT_SUFFIX_REGEX, "").replace("_", "")
        return try {
            BigDecimal(canonical).signum() != 0
        } catch (_: NumberFormatException) {
            true
        }
    }

    /** 官方 `src/Utils/FloatFormat.cpp:19-29` 的位域常量。 */
    private const val FLOAT32_EXP_MASK = 0x7F800000
    private const val FLOAT32_TAIL_MASK = 0x007FFFFF
    private const val FLOAT32_TAIL_WIDTH = 23
    private const val FLOAT32_EXP_BASE = 0b01111111
    private const val FLOAT16_SIGN_MASK = 0x8000
    private const val FLOAT16_EXP_BASE = 0b01111
    private const val FLOAT16_EXP_MAX = 0b11110
    private const val FLOAT16_TAIL_WIDTH = 10
    private const val FLOAT16_INF = 0b11111 shl FLOAT16_TAIL_WIDTH
    private const val FLOAT32_FLOAT16_TAIL_OFFSET = FLOAT32_TAIL_WIDTH - FLOAT16_TAIL_WIDTH
}
