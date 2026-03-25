/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.components.TypeVariableDirectionCalculator.ResolveDirection
import org.cangnova.cangjie.resolve.calls.inference.extractTypeForGivenRecursiveTypeParameter
import org.cangnova.cangjie.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.cangnova.cangjie.resolve.calls.inference.isEqualityConstraintCompatible
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.TypeApproximatorConfiguration

class ResultTypeResolver(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemBuilder {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val outerSystemVariablesPrefixSize: Int
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
    }

    context(c: Context)
    private fun TypeVariableMarker.getDefaultTypeForSelfType(constraints: List<Constraint>): CangJieTypeMarker? {
        val typeVariableConstructor = freshTypeConstructor()
        val typeParameter = typeVariableConstructor.typeParameter ?: return null

        val typesForRecursiveTypeParameters = constraints.mapNotNull { constraint ->
            if (constraint.position.from !is DeclaredUpperBoundConstraintPosition<*>) return@mapNotNull null
            constraint.type.extractTypeForGivenRecursiveTypeParameter(typeParameter)
        }.takeIf { it.isNotEmpty() } ?: return null

        return c.createCapturedPlaceholderTypeForSelfType(typeVariableConstructor, typesForRecursiveTypeParameters)
    }

    context(c: Context)
    private fun TypeVariableMarker.getDefaultType(direction: ResolveDirection, constraints: List<Constraint>): CangJieTypeMarker {
        getDefaultTypeForSelfType(constraints)?.let { return it }

        return if (direction == ResolveDirection.TO_SUBTYPE) c.nothingType() else c.anyType()
    }

    context(c: Context)
    fun findResultType(variableWithConstraints: VariableWithConstraints, direction: ResolveDirection): CangJieTypeMarker {
        findResultTypeOrNull(variableWithConstraints, direction)?.let { return it }

        // no proper constraints
        return variableWithConstraints.typeVariable.getDefaultType(direction, variableWithConstraints.constraints)
    }

    context(c: Context)
    private fun CangJieTypeMarker.approximateToSuperTypeOrSelf(superTypeCandidate: CangJieTypeMarker?): CangJieTypeMarker {
        // In case we have an ILT as the subtype, we approximate it using the upper type as the expected type.
        // This is more precise than always approximating it to Int or UInt.
        // Note, we shouldn't have nested ILTs because they can only appear as a constraint on a type variable
        // that we would have fixed earlier.
        if (typeConstructor().isIntegerLiteralTypeConstructor()) {
            return typeApproximator.approximateToSuperType(
                this,
                TypeApproximatorConfiguration.TopLevelIntegerLiteralTypeApproximationWithExpectedType(superTypeCandidate)
            ) ?: this
        }

        return typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.InternalTypesApproximation) ?: this
    }

    private fun CangJieTypeMarker.approximateToSubTypeOrSelf(): CangJieTypeMarker {
        return typeApproximator.approximateToSubType(this, TypeApproximatorConfiguration.InternalTypesApproximation) ?: this
    }

    context(c: Context)
    fun findResultTypeOrNull(
        variableWithConstraints: VariableWithConstraints,
        direction: ResolveDirection,
    ): CangJieTypeMarker? {
        val resultTypeFromEqualConstraint = findResultIfThereIsEqualsConstraint(variableWithConstraints, isStrictMode = false)
        if (resultTypeFromEqualConstraint?.isAppropriateResultTypeFromEqualityConstraints() == true) return resultTypeFromEqualConstraint

        val subType = variableWithConstraints.findSubType()
        val superType = variableWithConstraints.findSuperType()

        val (preparedSubType, preparedSuperType) = variableWithConstraints.prepareSubAndSuperTypes(subType, superType)

        val resultTypeFromDirection = if (direction == ResolveDirection.TO_SUBTYPE || direction == ResolveDirection.UNKNOWN) {
            variableWithConstraints.resultType(preparedSubType, preparedSuperType)
        } else {
            variableWithConstraints.resultType(preparedSuperType, preparedSubType)
        }

        // In the general case, we can have here two types, one from EQUAL constraint which must be ILT-based,
        // and the second one from UPPER/LOWER constraints (subType/superType based)
        // The logic of choice here is:
        // - if one type is null, we return another one
        // - we return type from UPPER/LOWER constraints if it's more precise (in fact, only Int/Short/Byte/Long is allowed here)
        // - otherwise we return ILT-based type
        return when {
            resultTypeFromEqualConstraint == null -> resultTypeFromDirection
            resultTypeFromDirection == null -> resultTypeFromEqualConstraint
            !resultTypeFromDirection.typeConstructor().isNothingConstructor() &&
                    AbstractTypeChecker.isSubtypeOf(c, resultTypeFromDirection, resultTypeFromEqualConstraint) -> resultTypeFromDirection
            else -> resultTypeFromEqualConstraint
        }
    }

    context(c: Context)
    private fun CangJieTypeMarker.isAppropriateResultTypeFromEqualityConstraints(): Boolean {
        return !contains { type ->
            type.typeConstructor().isIntegerLiteralConstantTypeConstructor()
        }
    }

    /**
     * The general approach to approximation of resulting types (in K2) is to
     * - always approximate ILTs
     * - always approximate captured types unless this leads to a contradiction.
     * A contradiction can appear if we have some captured type C = CapturedType(*) in the subtype and in the supertype.
     *
     * Example: A<C> <: T <: A<C>
     *
     * If we were to approximate the result type, we would end up with a contradiction
     * A<*> </: A<C>
     *
     * In comparison, types from equality constraints are never approximated because it would always lead to a contradiction.
     * We evaluated a never-approximate approach but found it to be infeasible as it introduces many new errors
     * (type mismatches, REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR, etc.).
     */
    context(c: Context)
    private fun VariableWithConstraints.prepareSubAndSuperTypes(
        subType: CangJieTypeMarker?,
        superType: CangJieTypeMarker?,
    ): Pair<CangJieTypeMarker?, CangJieTypeMarker?> {
        val approximatedSubType = subType?.approximateToSuperTypeOrSelf(superType)
        val approximatedSuperType = superType?.approximateToSubTypeOrSelf()

        val preparedSubType = when {
            approximatedSubType == null -> null
            shouldBeUsedWithoutApproximation(subType, approximatedSubType, this) -> subType
            else -> approximatedSubType
        }

        val preparedSuperType = when {
            approximatedSuperType == null -> null
            shouldBeUsedWithoutApproximation(superType, approximatedSuperType, this) -> superType
            superType.typeConstructor().hasRecursiveTypeParametersWithGivenSelfType() -> superType
            else -> approximatedSuperType
            // Super type should be the most flexible, sub type should be the least one
        }

        return preparedSubType to preparedSuperType
    }

    /**
     * Returns `true` if using [approximatedResultType] as result type leads to a contradiction.
     *
     * If [resultType] and [approximatedResultType] are referentially equal, it means there is nothing to approximate in the first place.
     * Therefore `false` is returned.
     *
     * Only used when [LanguageFeature.ImprovedCapturedTypeApproximationInInference] is enabled.
     */
    context(c: Context)
    private fun shouldBeUsedWithoutApproximation(
        resultType: CangJieTypeMarker,
        approximatedResultType: CangJieTypeMarker,
        variableWithConstraints: VariableWithConstraints,
    ): Boolean {
        if (resultType === approximatedResultType || c.hasContradiction) return false

        // TODO(related to KT-64802) This if shouldn't be necessary but removing it breaks
        // compiler/testData/diagnostics/tests/unsignedTypes/conversions/inferenceForSignedAndUnsignedTypes.kt
        if (resultType.typeConstructor().isIntegerLiteralTypeConstructor()) return false

        return !c.isEqualityConstraintCompatible(approximatedResultType, variableWithConstraints.typeVariable.defaultType())
    }

    context(c: Context)
    private fun VariableWithConstraints.resultType(
        firstCandidate: CangJieTypeMarker?,
        secondCandidate: CangJieTypeMarker?,
    ): CangJieTypeMarker? {
        if (firstCandidate == null || secondCandidate == null) return firstCandidate ?: secondCandidate

        specialResultForIntersectionType(firstCandidate, secondCandidate)?.let { intersectionWithAlternative ->
            return intersectionWithAlternative
        }

        if (isSuitableType(firstCandidate, this)) return firstCandidate

        return if (isSuitableType(secondCandidate, this)) {
            secondCandidate
        } else {
            firstCandidate
        }
    }

    context(c: Context)
    private fun specialResultForIntersectionType(firstCandidate: CangJieTypeMarker, secondCandidate: CangJieTypeMarker): CangJieTypeMarker? {
        if (firstCandidate.typeConstructor().isIntersection()) {
            if (!AbstractTypeChecker.isSubtypeOf(c, firstCandidate.toPublicType(), secondCandidate.toPublicType())) {
                return c.createTypeWithUpperBoundForIntersectionResult(firstCandidate, secondCandidate)
            }
        }

        return null
    }

    private fun CangJieTypeMarker.toPublicType(): CangJieTypeMarker =
        typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes) ?: this

    context(c: Context)
    private fun isSuitableType(resultType: CangJieTypeMarker, variableWithConstraints: VariableWithConstraints): Boolean {
        val filteredConstraints = variableWithConstraints.constraints.filter { it.isProperConstraint() }

        // TODO(KT-68213) this loop is only used for checking of incompatible ILT approximations in K1
        // It shouldn't be necessary in K2
        // but removing it breaks compiler/fir/analysis-tests/testData/resolve/inference/kt53494.kt
        for (constraint in filteredConstraints) {
            if (!checkConstraint(constraint.type, constraint.kind, resultType)) return false
        }

        // if resultType is not Nothing
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(resultType)) return true


        return filteredConstraints.any { it.kind.isUpper() }
    }

    context(c: Context)
    private fun VariableWithConstraints.findSubType(): CangJieTypeMarker? {
        val lowerConstraintTypes = prepareLowerConstraints(constraints)

        if (lowerConstraintTypes.isNotEmpty()) {
            if (lowerConstraintTypes.size > 1 &&
                lowerConstraintTypes.all { type ->
                    type.asRigidType()?.let { rigidType ->
                        rigidType.isStubTypeForVariableInSubtyping() || rigidType.isCapturedType()
                    } == true
                }
            ) {
                // This situation is only allowed to happen when semi-fixing for input types for OverloadResolutionByLambdaReturnType
                check(c.allowSemiFixationToOtherTypeVariables) {
                    "Only type-variable built constraints $lowerConstraintTypes found for $typeVariable"
                }
                return null
            }
            val types = sinkIntegerLiteralTypes(lowerConstraintTypes)
            var commonSuperType = CommonSuperTypeCalculator.commonSuperType(types)

            if (commonSuperType.contains { it.asRigidType()?.isStubTypeForVariableInSubtyping() == true }) {
                val typesWithoutStubs = types.filter { lowerType ->
                    !lowerType.contains { it.asRigidType()?.isStubTypeForVariableInSubtyping() == true }
                }

                when {
                    typesWithoutStubs.isNotEmpty() -> {
                        commonSuperType = CommonSuperTypeCalculator.commonSuperType(typesWithoutStubs)
                    }
                    // `typesWithoutStubs.isEmpty()` means that there are no lower constraints without type variables.
                    // It's only possible for the PCLA case, because otherwise none of the constraints would be considered as proper.
                    // So, we just get currently computed `commonSuperType` and substitute all local stub types
                    // with corresponding type variables.
                    c.outerSystemVariablesPrefixSize > 0 -> {
                        commonSuperType = c.createSubstitutionFromSubtypingStubTypesToTypeVariables().safeSubstitute(commonSuperType)
                    }
                }
            }

            return commonSuperType
        }

        return null
    }

    context(c: Context)
    private fun prepareLowerConstraints(constraints: List<Constraint>): List<CangJieTypeMarker> {
        var atLeastOneProper = false
        var atLeastOneNonProper = false

        val lowerConstraintTypes = mutableListOf<CangJieTypeMarker>()

        for (constraint in constraints) {
            if (constraint.kind != ConstraintKind.LOWER) continue
            if (constraint.isNoInfer) continue

            val type = constraint.type

            lowerConstraintTypes.add(type)

            if (type.isProperTypeForFixation()) {
                atLeastOneProper = true
            } else {
                atLeastOneNonProper = true
            }
        }

        if (!atLeastOneProper) return emptyList()

        // PCLA slow path
        // We only allow using TVs fixation for nested PCLA calls
        if (c.outerSystemVariablesPrefixSize > 0) {
            val notFixedToStubTypesSubstitutor = c.buildNotFixedVariablesToStubTypesSubstitutor()
            return lowerConstraintTypes.map { notFixedToStubTypesSubstitutor.safeSubstitute(it) }
        }

        if (!atLeastOneNonProper) return lowerConstraintTypes

        val notFixedToStubTypesSubstitutor = c.buildNotFixedVariablesToStubTypesSubstitutor()

        return lowerConstraintTypes.map { if (it.isProperTypeForFixation()) it else notFixedToStubTypesSubstitutor.safeSubstitute(it) }
    }

    context(c: Context)
    private fun sinkIntegerLiteralTypes(types: List<CangJieTypeMarker>): List<CangJieTypeMarker> {
        return types.sortedBy { type ->
            val containsILT = type.contains { it.asRigidType()?.isIntegerLiteralType() ?: false }
            if (containsILT) 1 else 0
        }
    }

    context(c: Context)
    private fun computeUpperType(upperConstraints: List<Constraint>): CangJieTypeMarker {
        return c.intersectTypes(upperConstraints.map { it.type })
    }

    context(c: Context)
    private fun VariableWithConstraints.findSuperType(): CangJieTypeMarker? {
        val upperConstraints = constraints.filter {
            if (it.kind != ConstraintKind.UPPER) return@filter false
            it.isProperConstraint()
        }

        if (upperConstraints.isNotEmpty()) {
            return computeUpperType(upperConstraints)
        }

        return null
    }

    context(c: Context)
    private fun Constraint.isProperConstraint(): Boolean {
        return type.isProperTypeForFixation() && !isNoInfer
    }

    context(c: Context)
    private fun CangJieTypeMarker.isProperTypeForFixation(): Boolean =
        isProperTypeForFixation(c.notFixedTypeVariables.keys) { c.isProperType(it) }

    context(c: Context)
    fun findResultIfThereIsEqualsConstraint(variableWithConstraints: VariableWithConstraints, isStrictMode: Boolean): CangJieTypeMarker? {
        val properEqualityConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.EQUALITY && it.isProperConstraint()
        }

        return representativeFromEqualityConstraints(variableWithConstraints, properEqualityConstraints, isStrictMode)
    }

    // Discriminate integer literal types as they are less specific than separate integer types (Int, Short...)
    context(c: Context)
    private fun representativeFromEqualityConstraints(
        variableWithConstraints: VariableWithConstraints,
        properEqualityConstraints: List<Constraint>,
        // Allow only types not-containing ILT and which might work as a representative of other ones from EQ constraints
        // TODO: Consider making it always `true` (see KT-70062)
        isStrictMode: Boolean
    ): CangJieTypeMarker? {
        if (properEqualityConstraints.isEmpty()) return null

        val constraintTypes = properEqualityConstraints.map { it.type }
        val nonLiteralTypes = constraintTypes.filter { constraintType ->
            if (isStrictMode)
                !constraintType.contains { it.typeConstructor().isIntegerLiteralTypeConstructor() }
            else
                !constraintType.typeConstructor().isIntegerLiteralTypeConstructor()
        }

        nonLiteralTypes.singleBestRepresentative()?.let { return it }

        if (isStrictMode) return null

        constraintTypes.singleBestRepresentative()?.let { return it }
        // At this point, it seems like the constraint system has found a contradiction.

        val typeVariable = variableWithConstraints.typeVariable
        // If there's a contradiction, it's best if we fix the variable to exactly
        // the type argument the user has provided for it, so that the diagnostics
        // remain accurate.
        return properEqualityConstraints.findExplicitTypeArgumentConstraintFor(typeVariable)?.type ?: constraintTypes.first()
    }

    /**
     * If there's a contradiction, type argument variables should be fixed to the explicit arguments
     * to make sure the error diagnostics and further checks in checkers remain accurate.
     */
    context(c: Context)
    private fun List<Constraint>.findExplicitTypeArgumentConstraintFor(typeVariable: TypeVariableMarker): Constraint? =
        firstOrNull {
            it.position.from is ExplicitTypeParameterConstraintPosition<*> && it.position.initialConstraint.a == typeVariable.defaultType()
        }
}
