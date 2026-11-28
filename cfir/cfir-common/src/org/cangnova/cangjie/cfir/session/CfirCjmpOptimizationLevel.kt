@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.session

import java.math.BigInteger

/**
 * CJMP common CJO 编译选项中的优化级别。
 *
 * `cjoValue` 与官方序列化枚举一一对应；命令行拼写只在 CLI 边界解析，
 * session、配置键及 CJO 读写在内部始终使用此类型。
 */
public enum class CfirCjmpOptimizationLevel(
    /** 官方 CJO `OptimizationLevel` 的稳定序号。 */
    public val cjoValue: UByte,
    /** 官方选项文案，用于加载门诊断。 */
    public val displayName: String,
) {
    O0(0u, "O0"),
    O1(1u, "O1"),
    O2(2u, "O2"),
    O3(3u, "O3"),
    O_S(4u, "Os"),
    O_Z(5u, "Oz"),
    ;

    public companion object {
        /**
         * 解析官方 `-O<value>` 后缀。
         *
         * 官方把数值 0–2 映射为对应等级、把大于 2 的数值映射为 O3；负数和非数字无效。
         * `s`、`z` 分别选择 size 与 size-aggressive 等级。
         */
        public fun fromCommandLineValue(value: String): CfirCjmpOptimizationLevel? {
            if (value == "s") return O_S
            if (value == "z") return O_Z
            if (value.isEmpty() || value.any { it !in '0'..'9' }) return null
            val numericValue = value.toBigIntegerOrNull() ?: return null
            return when {
                numericValue == BigInteger.ZERO -> O0
                numericValue == BigInteger.ONE -> O1
                numericValue == BigInteger.TWO -> O2
                else -> O3
            }
        }

        /** 从 CJO 中的官方枚举序号恢复级别；未知序号由加载门作为不兼容元数据拒绝。 */
        public fun fromCjoValueOrNull(value: UByte): CfirCjmpOptimizationLevel? =
            entries.singleOrNull { it.cjoValue == value }
    }
}
