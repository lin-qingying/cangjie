/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0
 */

package org.cangnova.cangjie.resolve.calls.inference.components

/**
 * 约束系统完成模式
 *
 * 控制类型推断完成阶段的行为，决定哪些延迟原子需要被分析、
 * 哪些类型变量需要被固定、以及 Fork 点约束是否需要被解析。
 *
 * ## 完成阶段概述
 *
 * 类型推断分为两个阶段：
 * 1. **收集阶段**：收集函数调用产生的约束
 * 2. **完成阶段**：根据约束固定类型变量，分析延迟原子（Lambda、函数引用等）
 *
 * 不同的完成模式对应不同的使用场景，在性能和完整性之间做出取舍。
 *
 * @param allPostponedAtomsShouldBeAnalyzed 是否要求分析所有延迟原子
 * @param allLambdasShouldBeAnalyzed 是否要求分析所有 Lambda（默认与前者一致）
 * @param shouldForkPointConstraintsBeResolved 是否解析 Fork 点约束
 * @param fixNotInferredTypeVariablesToErrorType 是否将无法推断的类型变量固定为错误类型
 */
enum class ConstraintSystemCompletionMode(
    val allPostponedAtomsShouldBeAnalyzed: Boolean,
    val allLambdasShouldBeAnalyzed: Boolean = allPostponedAtomsShouldBeAnalyzed,
    val shouldForkPointConstraintsBeResolved: Boolean,
    val fixNotInferredTypeVariablesToErrorType: Boolean,
) {

    /**
     * 完整完成模式
     *
     * 分析所有延迟原子，解析所有 Fork 点约束，
     * 将无法推断的类型变量固定为错误类型并报告诊断。
     *
     * ## 使用场景
     *
     * 普通函数调用的最终完成阶段，确保所有类型变量都被固定。
     *
     * ## 行为
     * - 分析所有 Lambda 和函数引用
     * - 解析所有 Fork 点约束
     * - 无法推断的类型变量 → 错误类型 + 报告 CANNOT_INFER_PARAMETER_TYPE
     */
    FULL(
        allPostponedAtomsShouldBeAnalyzed = true,
        shouldForkPointConstraintsBeResolved = true,
        fixNotInferredTypeVariablesToErrorType = true,
    ),

    /**
     * 部分完成模式
     *
     * 只固定当前已有足够信息的类型变量，不强制分析所有延迟原子。
     * 剩余的类型变量和未分析的原子留给后续阶段处理。
     *
     * ## 使用场景
     *
     * 嵌套调用中，外层调用需要先部分完成内层调用，
     * 才能获得足够信息进行重载选择。
     *
     * 例如：
     * ```
     * val x: Int64 = 1
     * x.plus(func() { x })
     * // 需要先分析 Lambda 才能选择正确的 plus 重载
     * // 但此时外层调用尚未完全确定，只能部分完成
     * ```
     *
     * ## 行为
     * - 只固定已有足够约束的类型变量
     * - 不强制分析 Lambda 和函数引用
     * - 不解析 Fork 点约束
     * - 不将未推断类型变量固定为错误类型
     */
    PARTIAL(
        allPostponedAtomsShouldBeAnalyzed = false,
        allLambdasShouldBeAnalyzed = false,
        shouldForkPointConstraintsBeResolved = false,
        fixNotInferredTypeVariablesToErrorType = false,
    ),

    /**
     * 遇到第一个 Lambda 时停止的完成模式
     *
     * 用于基于 Lambda 返回类型的重载解析场景。
     * 在遇到第一个需要分析的 Lambda 之前，尽可能多地固定类型变量。
     *
     * ## 使用场景
     *
     * 当多个重载函数只能通过 Lambda 返回类型区分时使用。
     * 此模式由重载解析器内部管理，不应在其他地方直接使用。
     *
     * ## 行为
     * - 遇到第一个 Lambda 时停止
     * - 解析 Fork 点约束（确保约束系统一致性）
     * - 不将未推断类型变量固定为错误类型
     *
     * **注意**：此模式仅供 [OverloadResolutionByLambdaReturnTypeResolver] 内部使用。
     * 如需判断当前是否处于此模式，请使用 [isUntilFirstLambda]。
     */
    @ExclusiveForOverloadResolutionByLambdaReturnType
    UNTIL_FIRST_LAMBDA(
        allPostponedAtomsShouldBeAnalyzed = false,
        allLambdasShouldBeAnalyzed = false,
        shouldForkPointConstraintsBeResolved = true,
        fixNotInferredTypeVariablesToErrorType = false,
    );

    /**
     * 判断当前是否为 [UNTIL_FIRST_LAMBDA] 模式
     *
     * 提供安全的模式判断方式，避免直接引用受限注解标记的枚举值。
     *
     * @return true 表示当前为遇到第一个 Lambda 时停止的模式
     */
    @OptIn(ExclusiveForOverloadResolutionByLambdaReturnType::class)
    fun isUntilFirstLambda(): Boolean = this == UNTIL_FIRST_LAMBDA

    init {
        // 不变式：如果要求分析所有延迟原子，则必然要求分析所有 Lambda
        assert(!allPostponedAtomsShouldBeAnalyzed || allLambdasShouldBeAnalyzed)
    }

    /**
     * 限制注解：标记仅供重载解析器使用的完成模式
     *
     * 带有此注解的枚举值（[UNTIL_FIRST_LAMBDA]）只应在
     * 基于 Lambda 返回类型的重载解析逻辑中使用。
     * 其他地方如需判断是否处于此模式，请使用 [isUntilFirstLambda]。
     */
    @RequiresOptIn(
        "此模式仅供 OverloadResolutionByLambdaReturnTypeResolver 使用。" +
                "如需判断当前模式，请使用 isUntilFirstLambda()。"
    )
    annotation class ExclusiveForOverloadResolutionByLambdaReturnType
}