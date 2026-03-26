/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability.*
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeKind
import org.cangnova.cangjie.type.model.*

interface OnlyInputTypeConstraintPosition

sealed class ConstraintPosition

abstract class ExplicitTypeParameterConstraintPosition<T>(val typeArgument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "TypeParameter $typeArgument"
}

abstract class InjectedAnotherStubTypeConstraintPosition<T>(private val builderInferenceLambdaOfInjectedStubType: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Injected from $builderInferenceLambdaOfInjectedStubType builder inference call"
}

abstract class BuilderInferenceSubstitutionConstraintPosition<L>(
    private val builderInferenceLambda: L,
    val initialConstraint: InitialConstraint,
    val isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Incorporated builder inference constraint $initialConstraint " +
            "into $builderInferenceLambda call"
}

abstract class ExpectedTypeConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "ExpectedType for call $topLevelCall"
}

abstract class DeclaredUpperBoundConstraintPosition<T>(val typeParameter: T) : ConstraintPosition() {
    override fun toString(): String = "DeclaredUpperBound $typeParameter"
}

abstract class CallableReferenceConstraintPosition<out T>(val call: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Callable reference $call"
}

abstract class ReceiverConstraintPosition<T>(val argument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Receiver $argument"
}

/**
 * The idea of this position is that sometimes we want to reserve the variable type, but it's not yet the moment when we call
 * [org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext.fixVariable], for example, we need to take
 * a look into a member scope of a type variable, but it's too early for fixation time because current result type may still contain
 * some other not fixed type variables, like `List<OtherTv>`.
 *
 * Currently, only used inside PCLA
 */
abstract class SemiFixVariableConstraintPosition(val variable: TypeVariableMarker) : ConstraintPosition() {
    override fun toString(): String = "Preliminary variable $variable fixation"
}

abstract class FixVariableConstraintPosition<T>(val variable: TypeVariableMarker, val resolvedAtom: T) : ConstraintPosition() {
    override fun toString(): String = "Fix variable $variable"
}

abstract class KnownTypeParameterConstraintPosition<T : CangJieTypeMarker>(val typeArgument: T) : ConstraintPosition() {
    override fun toString(): String = "TypeArgument $typeArgument"
}


sealed class ArgumentConstraintPosition<out T>(val argument: T) : ConstraintPosition()

abstract class RegularArgumentConstraintPosition<out T>(argument: T) : ArgumentConstraintPosition<T>(argument),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Argument $argument"
}

abstract class LambdaArgumentConstraintPosition<out T>(lambda: T) : ArgumentConstraintPosition<T>(lambda) {
    override fun toString(): String {
        return "LambdaArgument $argument"
    }
}

val <T> LambdaArgumentConstraintPosition<T>.lambda: T
    get() = argument

open class DelegatedPropertyConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition() {
    override fun toString(): String = "Constraint from call $topLevelCall for delegated property"
}

data class IncorporationConstraintPosition(
    val initialConstraint: InitialConstraint,
    var isFromDeclaredUpperBound: Boolean = false
) : ConstraintPosition() {
    val from: ConstraintPosition get() = initialConstraint.position

    override fun toString(): String = "Incorporate $initialConstraint from position $from"
}

object BuilderInferencePosition : ConstraintPosition() {
    override fun toString(): String = "For builder inference call"
}

data object ProvideDelegateFixationPosition : ConstraintPosition()

// TODO: should be used only in SimpleConstraintSystemImpl, KT-59675
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()

// ------------------------------------------------ Errors ------------------------------------------------

sealed class ConstraintSystemError(val applicability: CandidateApplicability)

sealed interface  ConstraintMismatch {
    val lowerType: CangJieTypeMarker
    val upperType: CangJieTypeMarker
    val position: IncorporationConstraintPosition
}

class  ConstraintError(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(if (position.from is ReceiverConstraintPosition<*>) INAPPLICABLE_WRONG_RECEIVER else INAPPLICABLE),
    ConstraintMismatch {
    override fun toString(): String {
        return "$lowerType <: $upperType"
    }
}

class ConstraintWarning(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(RESOLVED), ConstraintMismatch

open class NotEnoughInformationForTypeParameter<T>(
    val typeVariable: TypeVariableMarker,
    val resolvedAtom: T,
    val couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : ConstraintSystemError(INAPPLICABLE)

class InferredIntoDeclaredUpperBounds(val typeVariable: TypeVariableMarker) : ConstraintSystemError(RESOLVED)

class ConstrainingTypeIsError(
    val typeVariable: TypeVariableMarker,
    val constraintType: CangJieTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemError(INAPPLICABLE)

sealed interface InferredEmptyIntersection {
    val incompatibleTypes: List<CangJieTypeMarker>
    val causingTypes: List<CangJieTypeMarker>
    val typeVariable: TypeVariableMarker
    val kind: EmptyIntersectionTypeKind
}

class InferredEmptyIntersectionWarning(
    override val incompatibleTypes: List<CangJieTypeMarker>,
    override val causingTypes: List<CangJieTypeMarker>,
    override val typeVariable: TypeVariableMarker,
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(RESOLVED), InferredEmptyIntersection

class InferredEmptyIntersectionError(
    override val incompatibleTypes: List<CangJieTypeMarker>,
    override val causingTypes: List<CangJieTypeMarker>,
    override val typeVariable: TypeVariableMarker,
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(INAPPLICABLE), InferredEmptyIntersection

class OnlyInputTypesDiagnostic(val typeVariable: TypeVariableMarker) : ConstraintSystemError(INAPPLICABLE)

class LowerPriorityToPreserveCompatibility(val needToReportWarning: Boolean) :
    ConstraintSystemError(RESOLVED_NEED_PRESERVE_COMPATIBILITY)

open class MultiLambdaBuilderInferenceRestriction<T>(
    val anonymous: T,
    val typeParameter: TypeParameterMarker
) : ConstraintSystemError(RESOLVED_WITH_ERROR)

fun Constraint.isExpectedTypePosition() =
    position.from is ExpectedTypeConstraintPosition<*> || position.from is DelegatedPropertyConstraintPosition<*>

fun ConstraintError.transformToWarning() = ConstraintWarning(lowerType, upperType, position)
