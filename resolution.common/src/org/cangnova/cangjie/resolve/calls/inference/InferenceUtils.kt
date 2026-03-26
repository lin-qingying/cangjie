/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.type.model.*

fun ConstraintStorage.buildCurrentSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(fixedTypeVariables + additionalBindings)
}

fun ConstraintStorage.buildAbstractResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): TypeSubstitutorMarker = with(context) {
    if (allTypeVariables.isEmpty()) return createEmptySubstitutor()

    val uninferredSubstitutorMap = if (transformTypeVariablesToErrorTypes) {
        notFixedTypeVariables.entries.associate { (freshTypeConstructor, typeVariable) ->
            freshTypeConstructor to context.createUninferredType(
                with(context) { typeVariable.typeVariable.freshTypeConstructor() }
            )
        }
    } else {
        notFixedTypeVariables.entries.associate { (freshTypeConstructor, typeVariable) ->
            freshTypeConstructor to with(context) { typeVariable.typeVariable.defaultType() }
        }
    }
    return context.typeSubstitutorByTypeConstructor(fixedTypeVariables + uninferredSubstitutorMap)
}

fun ConstraintStorage.buildNotFixedVariablesToNonSubtypableTypesSubstitutor(
    context: TypeSystemInferenceExtensionContext
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(
        notFixedTypeVariables.entries.associate { (freshTypeConstructor, variableWithConstraints) ->
            freshTypeConstructor to context.createStubTypeForTypeVariablesInSubtyping(variableWithConstraints.typeVariable)
        }
    )
}

context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.hasRecursiveTypeParametersWithGivenSelfType(): Boolean {
    if (getParameters().any { it.hasRecursiveBounds(this) }) return true

    if (this.isIntersection()) {
        return supertypes().any {
            it.typeConstructor().hasRecursiveTypeParametersWithGivenSelfType()
        }
    }

    return false
}

context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.isRecursiveTypeParameter() =
    getTypeParameterClassifier()?.hasRecursiveBounds() == true

context(context: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.extractTypeForGivenRecursiveTypeParameter(typeParameter: TypeParameterMarker): CangJieTypeMarker? {
    for (argument in getArguments()) {
        val argumentType = argument.getType() ?: continue
        val typeConstructor = argumentType.typeConstructor()
        if (typeConstructor is TypeVariableTypeConstructorMarker
            && typeConstructor.typeParameter == typeParameter
            && typeConstructor.typeParameter?.hasRecursiveBounds(typeConstructor()) == true
        ) {
            return this
        }
        argumentType.extractTypeForGivenRecursiveTypeParameter(typeParameter)?.let { return it }
    }

    return null
}

fun ConstraintSystemImpl.registerTypeVariableIfNotPresent(
    typeVariable: TypeVariableMarker
) {
    val builder = getBuilder()
    if (with(this) { typeVariable.freshTypeConstructor() } !in builder.currentStorage().allTypeVariables.keys) {
        builder.registerVariable(typeVariable)
    }
}

context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.extractAllContainingTypeVariables(): Set<TypeConstructorMarker> = buildSet {
    extractAllContainingTypeVariablesNoCaptureTypeProcessing(this)
}

context(context: TypeSystemInferenceExtensionContext)
private fun CangJieTypeMarker.extractAllContainingTypeVariablesNoCaptureTypeProcessing(result: MutableSet<TypeConstructorMarker>) {
    contains { nestedType ->
        nestedType.typeConstructor().unwrapStubTypeVariableConstructor().let { nestedTypeConstructor ->
            if (nestedTypeConstructor.isTypeVariable()) {
                result.add(nestedTypeConstructor)
            }
        }
        false
    }
}
