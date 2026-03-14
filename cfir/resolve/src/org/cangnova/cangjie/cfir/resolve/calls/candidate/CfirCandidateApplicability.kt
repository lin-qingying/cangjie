package org.cangnova.cangjie.cfir.resolve.calls.candidate

/**
 * 候选适用性等级（7 级）。
 *
 * 值从低到高排列：HIDDEN 最差，RESOLVED 最优。
 * 用于候选收集器比较候选优劣，以及 Tower 遍历的停止条件判定。
 *
 * 对齐 K2 CandidateApplicability，去掉 SAM/suspend/smart-cast/K1-K2 兼容层等 Kotlin 特有层级。
 */
enum class CfirCandidateApplicability {

    /** 候选被隐藏（SinceKotlin / Deprecation 等，仓颉中主要用于内部 API 隐藏） */
    HIDDEN,

    /** 接收者类型不匹配 */
    INAPPLICABLE_WRONG_RECEIVER,

    /** 参数映射错误（参数数量不匹配） */
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,

    /** 参数类型不兼容等一般不适用 */
    INAPPLICABLE,

    /** 适用但优先级较低（低优先级重载标记） */
    RESOLVED_LOW_PRIORITY,

    /** 适用但有错误（例如可见性违规，仍可作为唯一候选报错） */
    RESOLVED_WITH_ERROR,

    /** 完全适用 */
    RESOLVED;

    /** 是否为成功适用（可作为最终候选） */
    val isSuccess: Boolean
        get() = this >= RESOLVED_LOW_PRIORITY

    /** 是否应停止 Tower 搜索（已找到足够好的候选） */
    val shouldStopResolve: Boolean
        get() = this >= RESOLVED_WITH_ERROR
}
