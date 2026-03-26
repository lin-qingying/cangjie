/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.type.model.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface ConstraintSystemOperation {
    val hasContradiction: Boolean
    fun registerVariable(variable: TypeVariableMarker)
    fun markPostponedVariable(variable: TypeVariableMarker)
    fun markCouldBeResolvedWithUnrestrictedBuilderInference()
    fun unmarkPostponedVariable(variable: TypeVariableMarker)
    fun removePostponedVariables()
    fun substituteFixedVariables(substitutor: TypeSubstitutorMarker)

    fun getBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>
    ): CangJieTypeMarker?

    fun getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable: TypeConstructorMarker): CangJieTypeMarker?

    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: CangJieTypeMarker
    )

    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: CangJieTypeMarker
    )

    fun addSubtypeConstraint(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker, position: ConstraintPosition)
    fun addEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition)

    fun isProperType(type: CangJieTypeMarker): Boolean
    fun isTypeVariable(type: CangJieTypeMarker): Boolean
    fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean


    fun addOtherSystem(otherSystem: ConstraintStorage)

    val errors: List<ConstraintSystemError>
}

abstract class ConstraintSystemTransaction {
    abstract fun closeTransaction()

    abstract fun rollbackTransaction()
}

interface ConstraintSystemBuilder : ConstraintSystemOperation {
    fun prepareTransaction(): ConstraintSystemTransaction

    fun buildCurrentSubstitutor(): TypeSubstitutorMarker

    fun currentStorage(): ConstraintStorage
}

// if runOperations return true, then this operation will be applied, and function return true
@OptIn(ExperimentalContracts::class)
inline fun ConstraintSystemBuilder.runTransaction(crossinline runOperations: ConstraintSystemOperation.() -> Boolean): Boolean {
    contract {
        callsInPlace(runOperations, InvocationKind.EXACTLY_ONCE)
    }
    val transactionState = prepareTransaction()

    // typeVariablesTransaction is clear
    if (runOperations()) {
        transactionState.closeTransaction()
        return true
    }

    transactionState.rollbackTransaction()
    return false
}

fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.LOWER)

fun ConstraintSystemBuilder.addEqualityConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.EQUALITY)

private fun ConstraintSystemBuilder.addConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition,
    kind: ConstraintKind
): Boolean = runTransaction {
    if (!hasContradiction) {
        when (kind) {
            ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
            ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
            ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
        }
    }
    !hasContradiction
}

fun ConstraintSystemBuilder.isSubtypeConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
): Boolean = isConstraintCompatible(lowerType, upperType, ConstraintKind.LOWER)

fun ConstraintSystemBuilder.isEqualityConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
): Boolean = isConstraintCompatible(lowerType, upperType, ConstraintKind.EQUALITY)

private fun ConstraintSystemBuilder.isConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    kind: ConstraintKind
): Boolean {
    var isCompatible = false
    runTransaction {
        if (!hasContradiction) {
            // the type of position is irrelevant since the constraint is always rolled back.
            val position = SimpleConstraintSystemConstraintPosition
            when (kind) {
                ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
                ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
                ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
            }
        }
        isCompatible = !hasContradiction
        false
    }
    return isCompatible
}

