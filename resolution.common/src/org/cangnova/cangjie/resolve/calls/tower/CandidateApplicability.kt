/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0
 */

package org.cangnova.cangjie.resolve.calls.tower

/**
 * 候选函数的适用性级别
 *
 * 描述在函数重载解析过程中，某个候选函数的匹配程度。
 * 枚举值按优先级从低到高排列，越靠后表示越优先被选择。
 *
 * ## 解析流程
 *
 * 塔式解析（Tower Resolve）会从最内层作用域向外逐层查找候选函数。
 * 当找到适用性达到 [shouldStopResolve] 的候选时，停止继续向外查找。
 *
 * ## 分组
 *
 * ```
 * ┌─────────────────────────────────────────────┐
 * │ 完全不适用（isSuccess = false）               │
 * │   HIDDEN                                     │
 * │   INAPPLICABLE_WRONG_RECEIVER                │
 * │   INAPPLICABLE_ARGUMENTS_MAPPING_ERROR       │
 * │   INAPPLICABLE                               │
 * │   VISIBILITY_ERROR                           │
 * │   UNSAFE_CALL                                │
 * │   UNSTABLE_SMARTCAST                         │
 * │   CONVENTION_ERROR                           │
 * ├─────────────────────────────────────────────┤
 * │ 成功但继续查找（isSuccess = true）             │
 * │   RESOLVED_LOW_PRIORITY                      │
 * │   RESOLVED_NEED_PRESERVE_COMPATIBILITY       │
 * ├─────────────────────────────────────────────┤
 * │ 成功且停止查找（shouldStopResolve = true）     │
 * │   RESOLVED_WITH_ERROR                        │
 * │   RESOLVED                                   │
 * └─────────────────────────────────────────────┘
 * ```
 */
enum class CandidateApplicability {

    /**
     * 候选函数因访问控制（如 deprecated hidden 级别）被隐藏，不可见。
     * 触发：UNRESOLVED_REFERENCE 诊断。
     */
    HIDDEN,

    /**
     * 候选函数的接收者类型不匹配。
     * 例如：对 Int 类型的变量调用只适用于 String 的扩展函数。
     */
    INAPPLICABLE_WRONG_RECEIVER,

    /**
     * 候选函数的参数无法映射到形参。
     * 例如：参数数量不匹配、缺少必要参数、多余的参数等。
     */
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,

    /**
     * 候选函数参数类型不匹配，或其他一般性不适用情况。
     * 例如：传入 String 但形参要求 Int64。
     */
    INAPPLICABLE,

    /**
     * 候选函数不可见。
     * 例如：访问了 private 或 internal 修饰的函数。
     * 触发：INVISIBLE_REFERENCE 诊断。
     */
    VISIBILITY_ERROR,

    /**
     * 候选函数可能适用，但接收者存在可空性问题。
     * 例如：对可空类型直接调用非空接收者的函数，而没有安全调用符 `?.`。
     */
    UNSAFE_CALL,

    /**
     * 候选函数可能适用，但需要不稳定的智能转换。
     * 例如：智能转换依赖于可能被修改的变量。
     */
    UNSTABLE_SMARTCAST,

    /**
     * 候选函数不符合调用约定。
     * 例如：以中缀方式调用没有 infix 修饰符的函数，
     * 或以操作符方式调用没有 operator 修饰符的函数。
     */
    CONVENTION_ERROR,

    // ── 以下适用性值的 isSuccess = true ──────────────────────────────

    /**
     * 候选函数匹配成功，但优先级较低。
     * 塔式解析会继续向外层作用域查找，以寻找更优先的候选。
     * 例如：通过隐式接收者找到的函数，优先级低于直接调用。
     */
    RESOLVED_LOW_PRIORITY,

    /**
     * 候选函数匹配成功，但使用了需要保持兼容性的新特性。
     * 塔式解析会继续向外层作用域查找。
     */
    RESOLVED_NEED_PRESERVE_COMPATIBILITY,

    // ── 以下适用性值的 shouldStopResolve = true ──────────────────────

    /**
     * 候选函数匹配成功，但存在错误（如类型推断失败）。
     * 塔式解析停止，但仍会报告错误诊断。
     *
     * 这是唯一一个既停止解析又触发错误的适用性级别。
     */
    RESOLVED_WITH_ERROR,

    /**
     * 候选函数完全匹配，或推断尚未完成（可能匹配成功）。
     * 塔式解析停止，选择该候选函数。
     */
    RESOLVED,
}

/**
 * 用于标记 [CandidateApplicability.isSuccess] 的访问需要明确意图。
 *
 * 提醒调用者注意：单个候选的 isSuccess 为 true，
 * 不代表整体解析结果成功（因为存在 RESOLVED_WITH_ERROR）。
 */
@RequiresOptIn
annotation class ApplicabilityDetail

/**
 * 判断该适用性级别是否表示候选函数匹配成功。
 *
 * **注意**：即使返回 true，也不代表最终解析无错误，
 * 因为 [CandidateApplicability.RESOLVED_WITH_ERROR] 也会返回 true。
 * 如果需要判断候选是否真正成功，请使用 `Candidate.isSuccessful`。
 *
 * @return true 表示候选匹配成功（含有错误的成功也算）
 */
@ApplicabilityDetail
val CandidateApplicability.isSuccess: Boolean
    get() = this >= CandidateApplicability.RESOLVED_LOW_PRIORITY &&
            this != CandidateApplicability.RESOLVED_WITH_ERROR

/**
 * 判断塔式解析是否应在此候选处停止，不再继续向外层作用域查找。
 *
 * 返回 true 时，解析器会以当前候选（或候选组）作为最终结果，
 * 即使该候选存在错误也会停止查找。
 *
 * @return true 表示应停止塔式解析
 */
val CandidateApplicability.shouldStopResolve: Boolean
    get() = this >= CandidateApplicability.RESOLVED_WITH_ERROR