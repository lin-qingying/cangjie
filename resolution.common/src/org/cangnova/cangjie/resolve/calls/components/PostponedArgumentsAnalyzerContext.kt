/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.UnstableSystemMergeMode
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.type.model.*

/**
 * 延迟参数分析阶段可访问的约束系统上下文。
 */
interface PostponedArgumentsAnalyzerContext : TypeSystemInferenceExtensionContext {
    /**
     * 当前尚未固定的类型变量及其约束。
     */
    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    /**
     * 使用单个额外绑定构造当前 substitutor。
     */
    fun buildCurrentSubstitutor(additionalBinding: Pair<TypeConstructorMarker, CangJieTypeMarker>?): TypeSubstitutorMarker =
        buildCurrentSubstitutor(if (additionalBinding == null) emptyMap() else mapOf(additionalBinding))

    /**
     * 使用额外绑定构造当前 substitutor。
     */
    fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker
    /**
     * 构造把未固定变量替换为 stub 类型的 substitutor。
     */
    fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
    /**
     * 为延迟类型变量生成 stub 类型绑定。
     */
    fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker>

    // type can be proper if it not contains not fixed type variables
    /**
     * 判断类型是否可以视为 proper type。
     */
    fun canBeProper(type: CangJieTypeMarker): Boolean

    /**
     * 判断类型变量是否有 upper/equality Unit 约束。
     */
    fun hasUpperOrEqualUnitConstraint(type: CangJieTypeMarker): Boolean

    /**
     * 从约束中移除延迟类型变量。
     */
    fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>)

    // mutable operations
    /**
     * 将另一个约束系统追加到当前系统。
     */
    fun addOtherSystem(otherSystem: ConstraintStorage)

    /**
     * 以不稳定合并模式合并另一个约束系统。
     */
    @UnstableSystemMergeMode
    fun mergeOtherSystem(otherSystem: ConstraintStorage)

    /**
     * 返回当前约束系统 builder。
     */
    fun getBuilder(): ConstraintSystemBuilder
    /**
     * 解析 fork point 约束。
     */
    fun resolveForkPointsConstraints()
}
