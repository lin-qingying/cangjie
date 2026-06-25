package org.cangnova.cangjie.cfir.resolve.match.exhaustive

import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern

/**
 * match 穷尽性分析结果。
 */
sealed class ExhaustivenessResult {
    /** 已确认穷尽。 */
    data object Exhaustive : ExhaustivenessResult()

    /**
     * 未穷尽结果。
     *
     * @property missingPatterns 可用于诊断展示的缺失模式列表。
     * @property source 产出该结果的 checker 来源。
     */
    data class NonExhaustive(
        /**
         * 可用于诊断展示的缺失模式列表。
         */
        val missingPatterns: List<CfirMatchPattern>,
        /**
         * 产出该结果的 checker 来源。
         */
        val source: CheckSource = CheckSource.UNKNOWN,
    ) : ExhaustivenessResult() {
        /**
         * 返回缺失模式文本列表。
         */
        fun getMissingPatternTexts(): List<String> = missingPatterns.map { it.text() }
    }

    /**
     * 分析错误结果。
     *
     * @property reason 错误原因。
     */
    data class Error(val reason: String) : ExhaustivenessResult()

    /** 当前 checker 跳过分析。 */
    data object Skipped : ExhaustivenessResult()

    /** 当前结果是否表示穷尽。 */
    val isExhaustive: Boolean
        get() = this is Exhaustive

    /** 当前结果是否表示未穷尽。 */
    val isNonExhaustive: Boolean
        get() = this is NonExhaustive
}

/**
 * 穷尽性分析来源。
 */
enum class CheckSource {
    /** 未知或未指定来源。 */
    UNKNOWN,
    /** 平凡检查器。 */
    TRIVIAL,
    /** 布尔专用检查器。 */
    BOOLEAN_FLAG,
    /** 小 enum 位图检查器。 */
    ENUM_BITVECTOR,
    /** 整数区间检查器。 */
    INTEGER_INTERVAL,
    /** 字符区间检查器。 */
    CHAR_INTERVAL,
    /** 元组分量检查器。 */
    TUPLE_COMPONENT,
    /** 嵌套展开检查器。 */
    NESTED_FLATTEN,
    /** Maranget 通用算法检查器。 */
    MARANGET,
}
