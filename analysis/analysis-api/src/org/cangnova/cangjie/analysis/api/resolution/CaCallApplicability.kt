package org.cangnova.cangjie.analysis.api.resolution

/**
 * 调用候选的适用性结果。
 *
 * 用于刻画"某个候选是否可被选中、不能选中的原因是什么",
 * 顺序按优先级递增:越靠后,表示越接近"完全成功"。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallApplicability` 思路。
 */
enum class CaCallApplicability {
    /**
     * 候选对当前作用域不可见(显式隐藏 / `@Hidden` 等场景)。
     */
    HIDDEN,

    /**
     * Receiver 类型不匹配。
     */
    INAPPLICABLE_WRONG_RECEIVER,

    /**
     * 实参与形参映射失败(数量、名字、上下文实参不匹配等)。
     */
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,

    /**
     * 其他形式的不适用。
     */
    INAPPLICABLE,

    /**
     * 因可见性约束被排除。
     */
    VISIBILITY_ERROR,

    /**
     * 不安全调用(例如 nullable 上未做空检查)。
     */
    UNSAFE_CALL,

    /**
     * Smart-cast 路径不稳定,不能依赖。
     */
    UNSTABLE_SMARTCAST,

    /**
     * 操作符约定不满足。
     */
    CONVENTION_ERROR,

    /**
     * 解析成功,但优先级低(仅作为兜底候选)。
     */
    RESOLVED_LOW_PRIORITY,

    /**
     * 解析成功,但需要保留旧版本兼容性(例如修复 BC 问题)。
     */
    RESOLVED_NEED_PRESERVE_COMPATIBILITY,

    /**
     * 解析"完成"但伴随错误(典型如类型不匹配但仍被选中作为最优候选)。
     */
    RESOLVED_WITH_ERROR,

    /**
     * 完全成功的调用解析。
     */
    RESOLVED,
}

/**
 * 当前适用性是否被视为"成功"。
 *
 * 标准:优先级不低于 [CaCallApplicability.RESOLVED_LOW_PRIORITY],
 * 且不是 [CaCallApplicability.RESOLVED_WITH_ERROR]。
 */
val CaCallApplicability.isSuccess: Boolean
    get() = this >= CaCallApplicability.RESOLVED_LOW_PRIORITY &&
        this != CaCallApplicability.RESOLVED_WITH_ERROR
