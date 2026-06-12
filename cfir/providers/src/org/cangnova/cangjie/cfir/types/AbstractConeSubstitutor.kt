/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeSubstitutorMarker

abstract class AbstractConeSubstitutor(
    protected val typeContext: ConeTypeContext,
) : ConeSubstitutor() {
    abstract fun substituteType(type: ConeCangJieType): ConeCangJieType?

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val newType = substituteOrNull(projection.type) ?: return null
        return newType
    }

    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
        val substitutedType = substituteType(type) ?: type.substituteRecursive()
        val substitutedAttributes = type.attributes.transformTypesWith(this::substituteOrNull)

        return when {
            substitutedType != null && substitutedAttributes != null -> {
                substitutedType.withAttributes(substitutedAttributes)
            }
            substitutedType != null -> substitutedType
            substitutedAttributes != null -> type.withAttributes(substitutedAttributes)
            else -> null
        }
    }

    private fun ConeCangJieType.substituteRecursive(): ConeCangJieType? {
        return when (this) {
            is ConeErrorType -> substituteErrorType()
            is ConeTypeAliasType -> substituteTypeAlias()
            is ConeFunctionType -> substituteFunctionType()
            is ConeTupleType -> substituteTupleType()
            is ConeVArrayType -> substituteVArrayType()
            is ConePointerType -> substitutePointerType()
            is ConeIntersectionType -> substituteIntersectionType()
            is ConeUnionType -> substituteUnionType()
            is ConeRigidType -> substituteArguments()
            else -> null
        }
    }

    private fun ConeErrorType.substituteErrorType(): ConeCangJieType? {
        val substitutedDelegatedType = delegatedType?.let { substituteOrNull(it) ?: it }
        val substitutedArguments = substituteArguments()
        if (substitutedDelegatedType == delegatedType && substitutedArguments == null) return null

        return ConeErrorType(
            diagnostic = diagnostic,
            isUninferredParameter = isUninferredParameter,
            delegatedType = substitutedDelegatedType,
            typeArguments = (substitutedArguments as? ConeErrorType)?.typeArguments ?: typeArguments,
            attributes = attributes,
        )
    }

    private fun ConeTypeAliasType.substituteTypeAlias(): ConeCangJieType? {
        val substitutedExpandedType = substituteOrNull(expandedType)
        val substitutedArguments = substituteArguments()
        if (substitutedExpandedType == null && substitutedArguments == null) return null

        val arguments = (substitutedArguments as? ConeTypeAliasType)?.typeArguments ?: typeArguments
        return ConeTypeAliasType(classId, substitutedExpandedType ?: expandedType, arguments, attributes)
    }

    /**
     * 仓颉的函数、元组、VArray、指针、交叉与联合类型是结构类型，
     * 组成类型存放在专用字段中；替换器必须递归进入这些字段。
     */
    private fun ConeFunctionType.substituteFunctionType(): ConeCangJieType? {
        val substitutedParameterTypes = substituteTypes(parameterTypes)
        val substitutedReturnType = substituteOrNull(returnType)
        if (substitutedParameterTypes == null && substitutedReturnType == null) return null

        return ConeFunctionType(
            parameterTypes = substitutedParameterTypes ?: parameterTypes,
            returnType = substitutedReturnType ?: returnType,
            isCFunc = isCFunc,
            isClosureType = isClosureType,
            hasVariableLenArg = hasVariableLenArg,
            attributes = attributes,
        )
    }

    private fun ConeTupleType.substituteTupleType(): ConeCangJieType? {
        val substitutedElements = substituteTypes(elementTypes) ?: return null
        return ConeTupleType(substitutedElements, attributes)
    }

    private fun ConeVArrayType.substituteVArrayType(): ConeCangJieType? {
        val substitutedElementType = substituteOrNull(elementType) ?: return null
        return ConeVArrayType(substitutedElementType, size, attributes)
    }

    private fun ConePointerType.substitutePointerType(): ConeCangJieType? {
        val substitutedPointeeType = substituteOrNull(pointeeType) ?: return null
        return ConePointerType(substitutedPointeeType, attributes)
    }

    private fun ConeIntersectionType.substituteIntersectionType(): ConeCangJieType? {
        val substitutedIntersectedTypes = substituteTypes(intersectedTypes)
        val substitutedUpperBound = upperBoundForApproximation?.let { substituteOrNull(it) ?: it }
        if (substitutedIntersectedTypes == null && substitutedUpperBound == upperBoundForApproximation) return null

        return ConeIntersectionType(
            intersectedTypes = substitutedIntersectedTypes ?: intersectedTypes,
            upperBoundForApproximation = substitutedUpperBound,
            attributes = attributes,
        )
    }

    private fun ConeUnionType.substituteUnionType(): ConeCangJieType? {
        val substitutedUnionTypes = substituteTypes(unionTypes) ?: return null
        return ConeUnionType(substitutedUnionTypes.toSet(), attributes)
    }

    private fun substituteTypes(types: Collection<ConeCangJieType>): List<ConeCangJieType>? {
        var changed = false
        val substituted = types.map { type ->
            substituteOrNull(type)?.also { changed = true } ?: type
        }
        return substituted.takeIf { changed }
    }

    private fun ConeRigidType.substituteArguments(): ConeCangJieType? {
        val arguments = typeArguments
        if (arguments.isEmpty()) return null

        var changed = false
        val newArguments = ArrayList<ConeTypeProjection>(arguments.size)

        for ((index, argument) in arguments.withIndex()) {
            val substituted = substituteArgument(argument, index)
            if (substituted != null) {
                changed = true
                newArguments += substituted
            } else {
                newArguments += argument
            }
        }

        if (!changed) return null
        return withArguments(newArguments)
    }
}

fun createTypeSubstitutorByTypeConstructor(
    map: Map<TypeConstructorMarker, ConeCangJieType>,
    context: ConeTypeContext,
    approximateIntegerLiterals: Boolean,
): ConeSubstitutor {
    if (map.isEmpty()) return ConeSubstitutor.Empty
    return ConeTypeSubstitutorByTypeConstructor(map, context, approximateIntegerLiterals)
}

private class ConeTypeSubstitutorByTypeConstructor(
    private val map: Map<TypeConstructorMarker, ConeCangJieType>,
    typeContext: ConeTypeContext,
    private val approximateIntegerLiterals: Boolean,
) : AbstractConeSubstitutor(typeContext) {

    override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
        val constructor = type.typeConstructorForSubstitution() ?: return null
        val newType = map[constructor] ?: return null
        return if (approximateIntegerLiterals) newType.approximateIntegerLiteralType() else newType
    }

    override fun toString(): String {
        return map.entries.joinToString(prefix = "{", postfix = "}", separator = " | ") { (constructor, type) ->
            "$constructor -> $type"
        }
    }
}

object ConeEmptySubstitutor : ConeSubstitutor() {
    override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = type

    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? = null

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? = null

    override fun toString(): String = "ConeEmptySubstitutor"
}

@Suppress("NOTHING_TO_INLINE")
inline fun TypeSubstitutorMarker.asCone(): ConeSubstitutor = this as ConeSubstitutor

@Deprecated(message = "This call is redundant, please just drop it", level = DeprecationLevel.ERROR)
fun ConeSubstitutor.asCone(): ConeSubstitutor = this

fun ConeSubstitutor.substituteOrNull(type: ConeCangJieType?): ConeCangJieType? {
    return type?.let { substituteOrNull(it) }
}

private fun ConeCangJieType.typeConstructorForSubstitution(): TypeConstructorMarker? {
    return when (this) {
        is ConeLookupTagBasedType -> lookupTag
        is ConeTypeVariableType -> typeConstructor
        is ConeStubType -> constructor
        is ConeTypeConstructorMarker -> this
        else -> null
    }
}

private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType {
    return when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        is ConePrimitiveType -> IdealTypeResolver.resolveIfIdeal(this)
        else -> this
    }
}
