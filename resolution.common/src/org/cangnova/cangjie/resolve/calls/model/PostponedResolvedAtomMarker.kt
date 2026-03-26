/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0
 */

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.type.model.CangJieTypeMarker

/**
 * 延迟解析原子的标记接口
 *
 * 在类型推断过程中，某些表达式无法立即完成类型推断，
 * 需要等待更多类型信息后再进行分析，这类表达式被称为"延迟解析原子"。
 *
 * ## 典型场景
 *
 * - **Lambda 表达式**：参数类型未知，需等待函数形参类型确定后再分析
 * - **函数引用**：目标函数重载未确定，需等待期望类型确定后再解析
 * - **集合字面量**：元素类型未知，需等待期望类型确定后再推断
 *
 * ## 工作流程
 *
 * ```
 * 1. 收集实参约束（跳过延迟原子）
 * 2. 固定可以确定的类型变量
 * 3. 用已确定的类型分析延迟原子
 * 4. 将分析结果作为新约束加入系统
 * 5. 重复直到所有原子分析完毕
 * ```
 */
interface PostponedResolvedAtomMarker {

    /**
     * 开始分析该原子所需的输入类型集合
     *
     * 只有当这些类型都成为"proper 类型"（不含未固定类型变量）时，
     * 才可以开始分析该原子。
     *
     * 通常是接收者类型和参数类型的列表。
     * 也可能包含期望类型对应的类型变量。
     *
     * 用于：
     * - 判断原子是否已准备好被分析
     * - 确定多个延迟原子之间的分析顺序
     */
    val inputTypes: Collection<CangJieTypeMarker>

    /**
     * 分析该原子后可能被细化的输出类型
     *
     * 分析完成后，可能会产生新的约束来细化这个类型。
     * 通常是 lambda 或函数引用的返回类型。
     *
     * 用于确定类型变量之间的依赖关系：
     * 如果某个类型变量出现在 outputType 中，
     * 说明它的约束可能在分析该原子后才能确定。
     *
     * 若返回类型未知或不相关则为 null。
     */
    val outputType: CangJieTypeMarker?

    /**
     * 该原子的期望类型
     *
     * 来自外部上下文的期望，例如：
     * - 函数形参要求的类型
     * - 变量声明的类型注解
     *
     * 用于在分析原子时提供类型提示。
     */
    val expectedType: CangJieTypeMarker?

    /**
     * 该原子是否已完成分析
     *
     * true 表示已分析完毕，约束已加入系统；
     * false 表示仍在等待分析。
     */
    val analyzed: Boolean
}

/**
 * 集合字面量延迟解析原子的标记接口
 *
 * 集合字面量（如 `[1, 2, 3]`）在元素类型未知时需要延迟解析，
 * 等待期望类型确定后再推断元素类型。
 */
interface CollectionLiteralAtomMarker : PostponedResolvedAtomMarker

/**
 * 支持修订期望类型的延迟解析原子接口
 *
 * 某些延迟原子在分析过程中，期望类型可能会被细化或修正。
 * 例如：函数引用在重载解析后，期望类型会从模糊变为精确。
 */
interface PostponedAtomWithRevisableExpectedType : PostponedResolvedAtomMarker {

    /**
     * 修订后的期望类型
     *
     * 初始为 null，在分析过程中通过 [reviseExpectedType] 设置。
     * 一旦设置，优先使用此类型而非原始的 [expectedType]。
     */
    val revisedExpectedType: CangJieTypeMarker?

    /**
     * 修订该原子的期望类型
     *
     * 当外部获得更精确的类型信息时调用，
     * 用新的期望类型替换原有的模糊期望类型。
     *
     * @param expectedType 新的期望类型
     */
    fun reviseExpectedType(expectedType: CangJieTypeMarker)
}

/**
 * 延迟解析的函数引用原子标记接口
 *
 * 函数引用（如 `::foo`）在目标重载未确定时需要延迟解析，
 * 等待期望函数类型确定后再选择具体的重载版本。
 *
 * ## 解析时机
 *
 * 当 [needsResolution] 为 true 时，表示函数引用仍需重载解析；
 * 当 [analyzed] 为 true 时，表示已完成解析，[needsResolution] 自动为 false。
 */
interface PostponedCallableReferenceMarker : PostponedAtomWithRevisableExpectedType {

    /**
     * 是否仍需进行重载解析
     *
     * true 表示函数引用尚未完成重载解析；
     * false 表示已完成解析（即 [analyzed] 为 true）。
     */
    val needsResolution get() = !analyzed
}

/**
 * 以类型变量作为期望类型的 Lambda 延迟解析原子标记接口
 *
 * 当 Lambda 的期望类型是一个尚未固定的类型变量时，
 * 无法立即确定 Lambda 的参数类型，需要延迟分析。
 *
 * ## 场景示例
 *
 * ```
 * func foo<F>(f: F) { }
 * foo { x -> x + 1 }
 * // F 是类型变量，Lambda 的期望类型未知
 * // 需要等待 F 被固定后，才能确定参数 x 的类型
 * ```
 *
 * 参数类型可能来自：
 * - Lambda 声明中的显式类型注解（如 `{ x: Int64 -> ... }`）
 * - 期望类型固定后推断出的类型
 */
interface LambdaWithTypeVariableAsExpectedTypeMarker : PostponedAtomWithRevisableExpectedType {

    /**
     * Lambda 声明中显式标注的参数类型列表
     *
     * 列表中每个元素对应一个参数：
     * - 非 null：该参数有显式类型注解
     * - null：该参数没有类型注解，需要从期望类型推断
     *
     * 整个列表为 null 表示尚未从声明中提取参数类型信息。
     */
    val parameterTypesFromDeclaration: List<CangJieTypeMarker?>?

    /**
     * 更新从 Lambda 声明中提取的参数类型列表
     *
     * 在分析 Lambda 声明时调用，将显式标注的参数类型记录下来，
     * 供后续类型推断使用。
     *
     * @param types 参数类型列表，null 表示无显式类型注解
     */
    fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?)
}