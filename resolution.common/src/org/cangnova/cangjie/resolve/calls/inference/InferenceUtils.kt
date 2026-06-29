/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.type.model.*

/**
 * 基于当前固定类型变量和额外绑定构造 substitutor。
 */
fun ConstraintStorage.buildCurrentSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(fixedTypeVariables + additionalBindings)
}

/**
 * 构造约束系统最终对外可见的抽象结果 substitutor。
 */
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

/**
 * 构造把未固定类型变量替换为不可参与子类型比较 stub 的 substitutor。
 */
fun ConstraintStorage.buildNotFixedVariablesToNonSubtypableTypesSubstitutor(
    context: TypeSystemInferenceExtensionContext
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(
        notFixedTypeVariables.entries.associate { (freshTypeConstructor, variableWithConstraints) ->
            freshTypeConstructor to context.createStubTypeForTypeVariablesInSubtyping(variableWithConstraints.typeVariable)
        }
    )
}

/**
 * 判断类型构造器是否存在以自身为界的递归类型参数。
 */
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

/**
 * 判断类型构造器对应的类型参数是否有递归边界。
 */
context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.isRecursiveTypeParameter() =
    getTypeParameterClassifier()?.hasRecursiveBounds() == true

/**
 * 从类型树中提取包含给定递归类型参数的外层类型。
 */
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

/**
 * 如果类型变量尚未注册到约束系统，则注册它。
 */
fun ConstraintSystemImpl.registerTypeVariableIfNotPresent(
    typeVariable: TypeVariableMarker
) {
    val builder = getBuilder()
    if (with(this) { typeVariable.freshTypeConstructor() } !in builder.currentStorage().allTypeVariables.keys) {
        builder.registerVariable(typeVariable)
    }
}

/**
 * 提取类型树中包含的所有类型变量构造器。
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.extractAllContainingTypeVariables(): Set<TypeConstructorMarker> = buildSet {
    extractAllContainingTypeVariablesNoCaptureTypeProcessing(this)
}

/**
 * 不展开 captured type，直接从类型树中收集类型变量构造器。
 */
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
