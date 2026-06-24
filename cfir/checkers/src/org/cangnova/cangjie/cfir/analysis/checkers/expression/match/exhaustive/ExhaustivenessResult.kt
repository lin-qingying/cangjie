package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive

import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern

/** match 穷尽性检查的统一结果模型。 */
sealed class ExhaustivenessResult {
    /** 所有可能输入均被分支覆盖。 */
    data object Exhaustive : ExhaustivenessResult()

    /** 存在未覆盖输入，并携带可展示的缺失模式 witness。 */
    data class NonExhaustive(
        /** 缺失模式列表。 */
        val missingPatterns: List<CfirMatchPattern>,
        /** 产出该结果的 checker 来源。 */
        val source: CheckSource = CheckSource.UNKNOWN,
    ) : ExhaustivenessResult() {
        /** 将缺失模式转换成诊断展示文本。 */
        fun getMissingPatternTexts(): List<String> = missingPatterns.map { it.text() }
    }

    /** checker 执行失败，携带内部原因文本。 */
    data class Error(val reason: String) : ExhaustivenessResult()
    /** 当前 checker 明确跳过本次输入。 */
    data object Skipped : ExhaustivenessResult()

    /** 当前结果是否表示穷尽。 */
    val isExhaustive: Boolean get() = this is Exhaustive
    /** 当前结果是否表示非穷尽。 */
    val isNonExhaustive: Boolean get() = this is NonExhaustive
}

/** 穷尽性结果的来源分类，同时参与 checker 调度优先级。 */
enum class CheckSource {
    /** 未知或未标注来源。 */
    UNKNOWN,
    /** 平凡 wildcard/empty 等快速判断。 */
    TRIVIAL,
    /** Boolean 枚举值位图检查。 */
    BOOLEAN_FLAG,
    /** enum 构造器位图检查。 */
    ENUM_BITVECTOR,
    /** 整数区间检查。 */
    INTEGER_INTERVAL,
    /** 字符区间检查。 */
    CHAR_INTERVAL,
    /** tuple 分量递归检查。 */
    TUPLE_COMPONENT,
    /** 嵌套模式扁平化检查。 */
    NESTED_FLATTEN,
    /** 完整 Maranget usefulness 算法。 */
    MARANGET,
}
