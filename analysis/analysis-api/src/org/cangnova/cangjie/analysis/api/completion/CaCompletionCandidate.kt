package org.cangnova.cangjie.analysis.api.completion

/**
 * Analysis API 对补全候选的稳定语义判断结果。
 *
 * 表示候选项相对当前上下文的可达性状态,
 * IDE 据此选择直接插入、追加 import 或彻底隐藏候选。
 */
enum class CaCompletionCandidateStatus {
    /** 候选项当前已可访问,无需任何额外动作即可直接插入。 */
    DIRECT,

    /** 候选项可用,但需要先插入对应的 import 才能引用。 */
    REQUIRES_IMPORT,

    /** 候选项在当前上下文不可见(权限、shadow 等原因),应从补全列表中隐藏。 */
    HIDDEN,
}
