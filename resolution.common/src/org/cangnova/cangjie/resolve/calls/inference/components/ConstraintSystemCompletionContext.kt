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

abstract class ConstraintSystemCompletionContext : VariableFixationFinder.Context, ResultTypeResolver.Context, ConstraintSystemMarker {
    abstract override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
    abstract override val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
    abstract override val postponedTypeVariables: List<TypeVariableMarker>

    abstract fun getBuilder(): ConstraintSystemBuilder

    // type can be proper if it not contains not fixed type variables
    abstract fun canBeProper(type: CangJieTypeMarker): Boolean

    abstract fun containsOnlyFixedOrPostponedVariables(type: CangJieTypeMarker): Boolean
    abstract fun containsOnlyFixedVariables(type: CangJieTypeMarker): Boolean

    // mutable operations
    abstract fun addError(error: ConstraintSystemError)

    abstract fun fixVariable(
        variable: TypeVariableMarker,
        resultType: CangJieTypeMarker,
        position: FixVariableConstraintPosition<*>,
    )

    abstract fun couldBeResolvedWithUnrestrictedBuilderInference(): Boolean
    abstract fun resolveForkPointsConstraints()

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

    fun <A : PostponedResolvedAtomMarker> hasLambdaToAnalyze(postponedArguments: List<A>): Boolean {
        return analyzeArgumentWithFixedParameterTypes(postponedArguments) {}
    }

    // Avoiding smart cast from filterIsInstanceOrNull looks dirty
    private fun <A : PostponedResolvedAtomMarker> findPostponedArgumentWithRevisableExpectedType(postponedArguments: List<A>): A? =
        postponedArguments.firstOrNull { argument -> argument is PostponedAtomWithRevisableExpectedType }

    private fun <T : PostponedResolvedAtomMarker> findPostponedArgumentWithFixedInputTypes(
        postponedArguments: List<T>
    ) = postponedArguments.firstOrNull { argument -> argument.inputTypes.all { containsOnlyFixedVariables(it) } }

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
    @K2Only
    abstract fun <R> withTypeVariablesThatAreCountedAsProperTypes(
        typeVariables: Set<TypeConstructorMarker>, allowSemiFixationToOtherTypeVariables: Boolean = false, block: () -> R
    ): R
}
