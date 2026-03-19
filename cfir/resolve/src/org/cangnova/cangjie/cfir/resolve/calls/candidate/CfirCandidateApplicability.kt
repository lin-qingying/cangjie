package org.cangnova.cangjie.cfir.resolve.calls.candidate

/**
 * 候选适用性等级，共 7 档。
 * 值从低到高依次表示从最差到最好，既用于比较候选优劣，
 * 也用于判断 tower 搜索是否可以提前停止。
 * 对齐 K2 `CandidateApplicability`，去掉 Kotlin 特有的若干细分等级。
 */
enum class CfirCandidateApplicability {

    /** 候选被隐藏，例如内部 API 不可见。 */
    HIDDEN,

    /** 接收者类型不匹配。 */
    INAPPLICABLE_WRONG_RECEIVER,

    /** 参数映射失败，通常是参数个数不匹配。 */
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,

    /** 参数类型不兼容等一般性不适用。 */
    INAPPLICABLE,

    /** 可用，但优先级较低。 */
    RESOLVED_LOW_PRIORITY,

    /** 可用但带错误，例如可见性违规。 */
    RESOLVED_WITH_ERROR,

    /** 完全适用。 */
    RESOLVED;

    /** 是否属于成功适用，可作为最终候选。 */
    val isSuccess: Boolean
        get() = this >= RESOLVED_LOW_PRIORITY

    /** 是否可以停止 tower 搜索。 */
    val shouldStopResolve: Boolean
        get() = this >= RESOLVED_WITH_ERROR
}

