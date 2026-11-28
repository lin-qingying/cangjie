package org.cangnova.cangjie.ide.completion.api

import org.cangnova.cangjie.ide.completion.api.lookup.ExpectedTypeMatch

/**
 * 期望类型的语义类别。
 *
 * 不同类别的期望类型对候选的要求不同：类型位置要求「候选本身是类型」，
 * 而表达式位置要求「候选的结果类型相容」。把两者混成一种判断，
 * 会在声明类型的位置错误地保留函数候选。
 */
enum class CompletionExpectedTypeKind {
    /** 表达式位置：候选的结果类型需要与期望类型相容。 */
    EXPRESSION,

    /** 类型位置：候选必须是一个类型。 */
    TYPE,

    /** 无确定期望类型。 */
    NONE,
}

/**
 * 补全位置的期望类型。
 *
 * [NONE] 是显式建模的结论而非「算不出来」：调用方据此跳过 SMART 过滤，
 * 保证 SMART 在无期望类型时与 BASIC 得到同一集合。
 */
data class CompletionExpectedType(
    val kind: CompletionExpectedTypeKind,
    /** 渲染后的类型文本；[CompletionExpectedTypeKind.NONE] 时为空串。 */
    val typeText: String = "",
) {
    companion object {
        /** 无期望类型。 */
        val None: CompletionExpectedType = CompletionExpectedType(CompletionExpectedTypeKind.NONE)
    }

    /** 是否有确定期望类型。 */
    val isKnown: Boolean
        get() = kind != CompletionExpectedTypeKind.NONE
}

/**
 * 补全结果过滤器。
 *
 * 过滤发生在分析会话内：期望类型相容性必须查 Analysis，不能推到界面期。
 */
interface CompletionFilter {
    /**
     * 按补全种类过滤候选。
     *
     * 实现必须满足：
     * 1. [CompletionKind.BASIC] 不做期望类型过滤，只做既有的可见性/遮蔽过滤；
     * 2. [CompletionKind.SMART] 在期望类型未知时**必须**返回与 BASIC 相同的集合，
     *    而不是在「算不出期望类型」时清空候选；
     * 3. 过滤只依据 [CompletionRequest.kind] 与期望类型，
     *    **不得**依据 [CompletionRequest.invocationCount] 放宽可见性：
     *    重复按 Tab 不构成访问权限。
     */
    fun apply(request: CompletionRequest, snapshots: List<CompletionSnapshot>): List<CompletionSnapshot>
}

/**
 * 期望类型查询。
 *
 * 实现走 Analysis 的表达式类型组件（类型位置、赋值右值、条件、返回值等），
 * **不得**在插件侧重写一套期望类型推断。
 */
interface CompletionExpectedTypeProvider {
    /** 查询给定请求位置的期望类型；无确定结果时返回 [CompletionExpectedType.None]。 */
    fun expectedTypeAt(request: CompletionRequest): CompletionExpectedType
}

/**
 * 按期望类型匹配结论标注候选排序数据。
 *
 * 匹配结论已在快照的 [CompletionSortData.expectedTypeMatch] 中冻结，
 * 这里只做一次提取，供过滤与测试复用。
 */
fun CompletionSnapshot.expectedTypeMatch(): ExpectedTypeMatch = sort.expectedTypeMatch