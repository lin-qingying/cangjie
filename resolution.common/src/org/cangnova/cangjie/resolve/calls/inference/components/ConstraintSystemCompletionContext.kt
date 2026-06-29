/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.model.CollectionLiteralAtomMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.type.model.*

/**
 * 约束系统 completion 阶段使用的上下文视图。
 */
abstract class ConstraintSystemCompletionContext : VariableFixationFinder.Context, ResultTypeResolver.Context, ConstraintSystemMarker {
    /** 尚未固定的类型变量。 */
    abstract override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
    /** 已固定的类型变量结果。 */
    abstract override val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
    /** 延迟类型变量列表。 */
    abstract override val postponedTypeVariables: List<TypeVariableMarker>

    /**
     * 返回当前约束系统 builder。
     */
    abstract fun getBuilder(): ConstraintSystemBuilder

    // type can be proper if it not contains not fixed type variables
    /**
     * 判断类型是否可作为 proper type 参与 completion。
     */
    abstract fun canBeProper(type: CangJieTypeMarker): Boolean

    /**
     * 判断类型是否只包含已固定或延迟变量。
     */
    abstract fun containsOnlyFixedOrPostponedVariables(type: CangJieTypeMarker): Boolean
    /**
     * 判断类型是否只包含已固定变量。
     */
    abstract fun containsOnlyFixedVariables(type: CangJieTypeMarker): Boolean

    // mutable operations
    /**
     * 向约束系统追加错误。
     */
    abstract fun addError(error: ConstraintSystemError)

    /**
     * 固定指定类型变量。
     */
    abstract fun fixVariable(
        variable: TypeVariableMarker,
        resultType: CangJieTypeMarker,
        position: FixVariableConstraintPosition<*>,
    )

    /**
     * 判断当前系统是否可通过 unrestricted builder inference 解析。
     */
    abstract fun couldBeResolvedWithUnrestrictedBuilderInference(): Boolean
    /**
     * 解析 fork point 约束。
     */
    abstract fun resolveForkPointsConstraints()

    /**
     * 查找并分析输入类型已固定的延迟参数。
     */
    fun <A : PostponedResolvedAtomMarker> analyzeArgumentWithFixedParameterTypes(
        postponedArguments: List<A>,
        analyze: (A) -> Unit
    ): Boolean {
        val argumentToAnalyze = findPostponedArgumentWithFixedInputTypes(postponedArguments)

        if (argumentToAnalyze != null) {
            analyze(argumentToAnalyze)
            return true
        }

        return false
    }

    /**
     * 根据 completion mode 选择下一个可分析延迟参数。
     */
    fun <A : PostponedResolvedAtomMarker> analyzeNextReadyPostponedArgument(
        postponedArguments: List<A>,
        completionMode: ConstraintSystemCompletionMode,
        analyze: (A) -> Unit
    ): Boolean {
        if (completionMode.allLambdasShouldBeAnalyzed) {
            val argumentWithTypeVariableAsExpectedType = findPostponedArgumentWithRevisableExpectedType(postponedArguments)

            if (argumentWithTypeVariableAsExpectedType != null) {
                analyze(argumentWithTypeVariableAsExpectedType)
                return true
            }
        }

        return analyzeArgumentWithFixedParameterTypes(postponedArguments, analyze)
    }

    /**
     * 分析剩余第一个尚未分析的延迟参数。
     */
    fun <A : PostponedResolvedAtomMarker> analyzeRemainingNotAnalyzedPostponedArgument(
        postponedArguments: List<A>,
        analyze: (A) -> Unit
    ): Boolean {
        val remainingNotAnalyzedPostponedArgument = postponedArguments.firstOrNull { !it.analyzed }

        if (remainingNotAnalyzedPostponedArgument != null) {
            analyze(remainingNotAnalyzedPostponedArgument)
            return true
        }

        return false
    }

    /**
     * 判断是否存在可立即分析的 lambda。
     */
    fun <A : PostponedResolvedAtomMarker> hasLambdaToAnalyze(postponedArguments: List<A>): Boolean {
        return analyzeArgumentWithFixedParameterTypes(postponedArguments) {}
    }

    // Avoiding smart cast from filterIsInstanceOrNull looks dirty
    /**
     * 查找拥有可修订期望类型的延迟参数。
     */
    private fun <A : PostponedResolvedAtomMarker> findPostponedArgumentWithRevisableExpectedType(postponedArguments: List<A>): A? =
        postponedArguments.firstOrNull { argument -> argument is PostponedAtomWithRevisableExpectedType }

    /**
     * 查找所有输入类型都已固定的延迟参数。
     */
    private fun <T : PostponedResolvedAtomMarker> findPostponedArgumentWithFixedInputTypes(
        postponedArguments: List<T>
    ) = postponedArguments.firstOrNull { argument -> argument.inputTypes.all { containsOnlyFixedVariables(it) } }

    /**
     * 从 upper 约束中提取需要检查空交叉的类型。
     */
    fun List<Constraint>.extractUpperTypesToCheckIntersectionEmptiness(): List<CangJieTypeMarker> =
        filter { constraint ->
            constraint.kind == ConstraintKind.UPPER && !constraint.type.contains {
                !it.typeConstructor().isClassTypeConstructor() && !it.typeConstructor().isTypeParameterTypeConstructor()
            }
        }.map { it.type }

    /**
     * @see [org.cangnova.cangjie.resolve.calls.inference.components.VariableFixationFinder.Context.typeVariablesThatAreCountedAsProperTypes]
     * @see [org.cangnova.cangjie.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
     */
    /**
     * 在指定类型变量临时视为 proper type 的作用域内执行代码。
     */
    abstract fun <R> withTypeVariablesThatAreCountedAsProperTypes(
        typeVariables: Set<TypeConstructorMarker>, allowSemiFixationToOtherTypeVariables: Boolean = false, block: () -> R
    ): R
}
