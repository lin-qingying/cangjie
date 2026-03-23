/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.TypeApproximatorCachesPerConfiguration
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.utils.SmartSet
import java.util.*

// todo problem: intersection types in constrains: A <: Number, B <: Inv<A & Any> =>? B <: Inv<out Number & Any>
class ConstraintIncorporator(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    val utilContext: ConstraintSystemUtilContext,
    private val languageVersionSettings: LanguageVersionSettings,
    inferenceLoggerParameter: InferenceLogger? = null,
) {
    /**
     * A workaround for K1's DI: the dummy instance must be provided, but
     * because it's useless, it's better to avoid calling its members to
     * prevent performance penalties.
     */
    val inferenceLogger = inferenceLoggerParameter.takeIf { it !is InferenceLogger.Dummy }

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        fun getVariablesWithConstraintsContainingGivenTypeVariable(
            variableConstructorMarker: TypeConstructorMarker,
        ): Collection<VariableWithConstraints>

        // if such a type variable is fixed then it is error
        fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker?

        fun getConstraintsForVariable(typeVariable: TypeVariableMarker): List<Constraint>

        // A <:(=) \alpha <:(=) B => A <: B
        fun processNewInitialConstraintFromIncorporation(
            // A
            lowerType: CangJieTypeMarker,
            // B
            upperType: CangJieTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            // Union of `derivedFrom` for `A <:(=) \alpha` and `\alpha <:(=) B`
            newDerivedFrom: Set<TypeVariableMarker>,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean,
        )

        fun addNewIncorporatedConstraint(typeVariable: TypeVariableMarker, type: CangJieTypeMarker, constraintContext: ConstraintContext)

        val approximatorCaches: TypeApproximatorCachesPerConfiguration
    }

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    context(c: Context)
    fun incorporate(typeVariable: TypeVariableMarker, constraint: Constraint) {
        // we shouldn't incorporate recursive constraint -- It is too dangerous
        if (constraint.areThereRecursiveConstraints(typeVariable)) return

        directWithVariable(typeVariable, constraint)
        insideOtherConstraint(typeVariable, constraint)
    }

    context(c: Context)
    private fun Constraint.areThereRecursiveConstraints(typeVariable: TypeVariableMarker) =
        type.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable.freshTypeConstructor() }

    // A <:(=) \alpha <:(=) B => A <: B
    context(c: Context)
    private fun directWithVariable(typeVariable: TypeVariableMarker, constraint: Constraint) {
        val shouldBeTypeVariableFlexible = with(utilContext) { typeVariable.shouldBeFlexible() }

        // \alpha <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            typeVariable.forEachConstraint {
                if (it !== constraint && it.kind != ConstraintKind.UPPER) {
                    inferenceLogger?.withOrigins(
                        typeVariable, it,
                        typeVariable, constraint,
                    ) {
                        c.processNewInitialConstraintFromIncorporation(
                            lowerType = it.type,
                            upperType = constraint.type,
                            shouldTryUseDifferentFlexibilityForUpperType = shouldBeTypeVariableFlexible,
                            newDerivedFrom = constraint.computeNewDerivedFrom(it),
                            isFromDeclaredUpperBound = false,
                            isNoInfer = constraint.isNoInfer || it.isNoInfer,
                        )
                    } ?: c.processNewInitialConstraintFromIncorporation(
                        lowerType = it.type,
                        upperType = constraint.type,
                        shouldTryUseDifferentFlexibilityForUpperType = shouldBeTypeVariableFlexible,
                        newDerivedFrom = constraint.computeNewDerivedFrom(it),
                        isFromDeclaredUpperBound = false,
                        isNoInfer = constraint.isNoInfer || it.isNoInfer,
                    )
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            typeVariable.forEachConstraint {
                if (it !== constraint && it.kind != ConstraintKind.LOWER) {
                    val isFromDeclaredUpperBound =
                        it.position.from is DeclaredUpperBoundConstraintPosition<*> && !it.type.typeConstructor().isTypeVariable()

                    inferenceLogger?.withOrigins(
                        typeVariable, constraint,
                        typeVariable, it,
                    ) {
                        c.processNewInitialConstraintFromIncorporation(
                            lowerType = constraint.type,
                            upperType = it.type,
                            shouldTryUseDifferentFlexibilityForUpperType = shouldBeTypeVariableFlexible,
                            newDerivedFrom = constraint.computeNewDerivedFrom(it),
                            isFromDeclaredUpperBound = isFromDeclaredUpperBound,
                            isNoInfer = constraint.isNoInfer || it.isNoInfer,
                        )
                    } ?: c.processNewInitialConstraintFromIncorporation(
                        lowerType = constraint.type,
                        upperType = it.type,
                        shouldTryUseDifferentFlexibilityForUpperType = shouldBeTypeVariableFlexible,
                        newDerivedFrom = constraint.computeNewDerivedFrom(it),
                        isFromDeclaredUpperBound = isFromDeclaredUpperBound,
                        isNoInfer = constraint.isNoInfer || it.isNoInfer,
                    )
                }
            }
        }
    }

    // NB: The result is reflexive
    private fun Constraint.computeNewDerivedFrom(other: Constraint): Set<TypeVariableMarker> =
        when {
            derivedFrom.isEmpty() -> other.derivedFrom
            other.derivedFrom.isEmpty() -> derivedFrom
            else -> derivedFrom + other.derivedFrom
        }

    context(c: Context)
    private inline fun TypeVariableMarker.forEachConstraint(action: (Constraint) -> Unit) {
        // We use an indexed loop because the collection might be modified during the iteration.
        // However, the only modification is appending, so we should be fine.
        val constraints = c.getConstraintsForVariable(this)
        var i = 0
        while (i < constraints.size) {
            action(constraints[i++])
        }
    }

    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    context(c: Context)
    private fun insideOtherConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint,
    ) {
        if (typeVariable in constraint.derivedFrom) return
        val freshTypeConstructor = typeVariable.freshTypeConstructor()
        for (storageForOtherVariable in c.getVariablesWithConstraintsContainingGivenTypeVariable(freshTypeConstructor)) {
            for (otherConstraint in storageForOtherVariable.getConstraintsContainedSpecifiedTypeVariable(freshTypeConstructor)) {
                inferenceLogger?.withOrigins(
                    typeVariable, constraint,
                    storageForOtherVariable.typeVariable, otherConstraint,
                ) {
                    generateNewConstraintForSecondIncorporationKind(
                        typeVariable,
                        constraint,
                        storageForOtherVariable.typeVariable,
                        otherConstraint
                    )
                } ?: generateNewConstraintForSecondIncorporationKind(
                    typeVariable,
                    constraint,
                    storageForOtherVariable.typeVariable,
                    otherConstraint,
                )
            }
        }
    }


    // By "Second" we mean `insideOtherConstraint` here
    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    context(c: Context)
    private fun generateNewConstraintForSecondIncorporationKind(
        // \alpha
        causeOfIncorporationVariable: TypeVariableMarker,
        // \alpha <: Number
        causeOfIncorporationConstraint: Constraint,
        // \beta
        otherVariable: TypeVariableMarker,
        // \beta <: Inv<\alpha>
        otherConstraint: Constraint,
    ) {
        if (causeOfIncorporationVariable in otherConstraint.derivedFrom) return
        val (type, needApproximation) = computeConstraintTypeForSecondIncorporationKind(
            causeOfIncorporationVariable, causeOfIncorporationConstraint, otherConstraint
        )

        fun prepareType(toSuper: Boolean): CangJieTypeMarker =
            when {
                needApproximation -> approximateCapturedTypes(type, toSuper)
                else -> type
            }

        if (otherConstraint.kind != ConstraintKind.LOWER) {
            addNewConstraintForSecondIncorporationKind(
                causeOfIncorporationVariable,
                causeOfIncorporationConstraint,
                otherVariable,
                otherConstraint,
                prepareType(true),
                isSubtype = false
            )
        }

        if (otherConstraint.kind != ConstraintKind.UPPER) {
            addNewConstraintForSecondIncorporationKind(
                causeOfIncorporationVariable,
                causeOfIncorporationConstraint,
                otherVariable,
                otherConstraint,
                prepareType(false),
                isSubtype = true
            )
        }
    }

    /**
     * By "Second" we mean `insideOtherConstraint` here
     * \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
     *  The second boolean component defines if further approximation is required.
     *
     *  @return `Pair(Inv<Captured(out Number)>, true)`
     */
    context(c: Context)
    private fun computeConstraintTypeForSecondIncorporationKind(
        // \alpha
        causeOfIncorporationVariable: TypeVariableMarker,
        // \alpha <: Number
        causeOfIncorporationConstraint: Constraint,
        // \beta <: Inv<\alpha>
        otherConstraint: Constraint,
    ): Pair<CangJieTypeMarker, Boolean> {
        val isBaseGenericType = otherConstraint.type.argumentsCount() != 0
        val isBaseOrOtherCapturedType = otherConstraint.type.isCapturedType() || causeOfIncorporationConstraint.type.isCapturedType()

        val (alphaReplacement, needsApproximation) = when (causeOfIncorporationConstraint.kind) {
            ConstraintKind.EQUALITY -> {
                causeOfIncorporationConstraint.type to false
            }
            ConstraintKind.UPPER -> {
                when (otherConstraint.kind) {
                    ConstraintKind.LOWER if !isBaseGenericType && !isBaseOrOtherCapturedType -> c.nothingType() to false
                    ConstraintKind.UPPER if !isBaseGenericType && !isBaseOrOtherCapturedType -> causeOfIncorporationConstraint.type to false
                    else -> c.createCapturedType(
                        c.createTypeArgument(causeOfIncorporationConstraint.type),
                        listOf(causeOfIncorporationConstraint.type),
                        null,
                        CaptureStatus.FOR_INCORPORATION
                    ) to true
                }
            }
            ConstraintKind.LOWER -> {
                when (otherConstraint.kind) {
                    ConstraintKind.UPPER if !isBaseGenericType && !isBaseOrOtherCapturedType -> c.anyType() to false
                    ConstraintKind.LOWER if !isBaseGenericType && !isBaseOrOtherCapturedType -> causeOfIncorporationConstraint.type to false
                    else -> c.createCapturedType(
                        c.createTypeArgument(causeOfIncorporationConstraint.type),
                        emptyList(),
                        causeOfIncorporationConstraint.type,
                        CaptureStatus.FOR_INCORPORATION
                    ) to true
                }
            }
        }

        return otherConstraint.type.substitute(causeOfIncorporationVariable, alphaReplacement) to needsApproximation
    }

    // By "Second" we mean `insideOtherConstraint` here
    // \alpha <: Number, \beta <: Inv<\alpha> => \beta <: Inv<out Number>
    context(c: Context)
    private fun addNewConstraintForSecondIncorporationKind(
        // \alpha
        causeOfIncorporationVariable: TypeVariableMarker,
        // \alpha <: Number
        causeOfIncorporationConstraint: Constraint,
        // \beta
        targetVariable: TypeVariableMarker,
        // \beta <: Inv<\alpha>
        otherConstraint: Constraint,
        // Inv<out Number>
        newConstraintType: CangJieTypeMarker,
        isSubtype: Boolean,
    ) {
        if (newConstraintType.containsNestedTypeVariable(targetVariable)) return

        val isFromVariableFixation = otherConstraint.position.from is FixVariableConstraintPosition<*>
                || causeOfIncorporationConstraint.position.from is FixVariableConstraintPosition<*>

        if (!causeOfIncorporationConstraint.kind.isEqual() &&
            !isFromVariableFixation &&
            !newConstraintType.containsConstrainingTypeWithoutProjection(causeOfIncorporationConstraint)
        ) return

        if (trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(
                otherConstraint, causeOfIncorporationConstraint, newConstraintType, isSubtype
            )
        ) return

        val derivedFrom = SmartSet.create(otherConstraint.derivedFrom).also { it.addAll(causeOfIncorporationConstraint.derivedFrom) }
        derivedFrom.add(causeOfIncorporationVariable)

        val kind = if (isSubtype) ConstraintKind.LOWER else ConstraintKind.UPPER

        val inputTypePosition =
            otherConstraint.position.from as? OnlyInputTypeConstraintPosition ?: otherConstraint.inputTypePositionBeforeIncorporation

        val constraintContext = ConstraintContext(
            kind = kind,
            derivedFrom = derivedFrom,
            inputTypePositionBeforeIncorporation = inputTypePosition,
            isNoInfer = causeOfIncorporationConstraint.isNoInfer || otherConstraint.isNoInfer
        )

        c.addNewIncorporatedConstraint(targetVariable, newConstraintType, constraintContext)
    }

    context(c: Context)
    private fun CangJieTypeMarker.containsConstrainingTypeWithoutProjection(otherConstraint: Constraint): Boolean {
        return anyNestedArgument {
            it.getType()?.typeConstructor() == otherConstraint.type.typeConstructor()
        }
    }

    context(c: Context)
    private fun CangJieTypeMarker.containsNestedTypeVariable(targetVariable: TypeVariableMarker): Boolean {
        return anyNestedArgument { typeArgument ->
            targetVariable == typeArgument.getType()?.let { c.getTypeVariable(it.typeConstructor().unwrapStubTypeVariableConstructor()) }
        }
    }

    context(c: Context)
    private fun CangJieTypeMarker.substitute(typeVariable: TypeVariableMarker, value: CangJieTypeMarker): CangJieTypeMarker {
        val substitutor = c.typeSubstitutorByTypeConstructor(mapOf(typeVariable.freshTypeConstructor() to value))
        return substitutor.safeSubstitute(this)
    }

    context(c: Context)
    private fun approximateCapturedTypes(type: CangJieTypeMarker, toSuper: Boolean): CangJieTypeMarker =
        when {
            toSuper -> typeApproximator.approximateToSuperType(
                type, TypeApproximatorConfiguration.IncorporationConfiguration,
                c.approximatorCaches,
            ) ?: type
            else -> typeApproximator.approximateToSubType(
                type, TypeApproximatorConfiguration.IncorporationConfiguration,
                c.approximatorCaches,
            ) ?: type
        }
}

context(c: TypeSystemInferenceExtensionContext)
private inline fun CangJieTypeMarker.anyNestedArgument(predicate: (TypeArgumentMarker) -> Boolean): Boolean {
    val stack = ArrayDeque<TypeArgumentMarker>()

    stack.push(c.createTypeArgument(this))

    val addArgumentsToStack = { projectedType: CangJieTypeMarker ->
        for (argumentIndex in 0 until projectedType.argumentsCount()) {
            stack.add(projectedType.getArgument(argumentIndex))
        }
    }

    while (!stack.isEmpty()) {
        val typeProjection = stack.pop()
        val typeProjectionType = typeProjection.getType() ?: continue

        if (predicate(typeProjection)) return true

        addArgumentsToStack(typeProjectionType)
    }

    return false
}
