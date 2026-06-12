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
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 基于声明侧类型参数替换表构造的 ConeSubstitutor。
 *
 * 与 Kotlin FIR `ConeSubstitutorByMap` 一致，替换键必须是类型参数的
 * constructor/symbol 身份，而不能是名称。不同声明中的同名类型参数在语义上
 * 是不同参数，按名称替换会错误捕获外层或其它声明的类型参数。
 *
 * 该能力属于通用类型系统基础设施，应放在 providers 层供 supertype 展开、
 * use-site scope 和调用解析共享，而不是局限在 resolve 某个阶段。
 */
class CfirTypeSubstitutorByMap(
    private val replacements: Map<TypeConstructorMarker, ConeCangJieType>,
) : ConeSubstitutor() {
    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
        return when (type) {
            is ConeTypeParameterType -> replacements[type.lookupTag]
            is ConeTypeVariableType -> replacements[type.typeConstructor]
            is ConeStubType -> replacements[type.constructor]
            is ConeClassLikeType -> substituteArguments(type.typeArguments)?.let { arguments ->
                ConeClassLikeType(type.lookupTag, arguments, type.attributes, type.isInterface, type.isThisType)
            }
            is ConeStructType -> substituteArguments(type.typeArguments)?.let { arguments ->
                ConeStructType(type.lookupTag, arguments, type.attributes)
            }
            is ConeEnumType -> substituteArguments(type.typeArguments)?.let { arguments ->
                ConeEnumType(type.lookupTag, arguments, type.attributes, type.isRefEnum)
            }
            is ConeFunctionType -> substituteFunction(type)
            is ConeTupleType -> substituteTypes(type.elementTypes)?.let { elements ->
                ConeTupleType(elements, type.attributes)
            }
            is ConeVArrayType -> substituteOrNull(type.elementType)?.let { elementType ->
                ConeVArrayType(elementType, type.size, type.attributes)
            }
            is ConePointerType -> substituteOrNull(type.pointeeType)?.let { pointeeType ->
                ConePointerType(pointeeType, type.attributes)
            }
            is ConeTypeAliasType -> substituteTypeAlias(type)
            is ConeIntersectionType -> substituteIntersectionType(type)
            is ConeUnionType -> substituteTypes(type.unionTypes.toList())?.let { unionTypes ->
                ConeUnionType(unionTypes.toSet(), type.attributes)
            }
            is ConeErrorType -> {
                val delegatedType = type.delegatedType?.let { substituteOrNull(it) ?: it }
                val typeArguments = substituteArguments(type.typeArguments)
                if (delegatedType == type.delegatedType && typeArguments == null) null
                else ConeErrorType(
                    diagnostic = type.diagnostic,
                    isUninferredParameter = type.isUninferredParameter,
                    delegatedType = delegatedType,
                    typeArguments = typeArguments ?: type.typeArguments,
                    attributes = type.attributes,
                )
            }
            else -> null
        }
    }

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        return substituteOrNull(projection.type)
    }

    private fun substituteFunction(type: ConeFunctionType): ConeFunctionType? {
        val parameterTypes = substituteTypes(type.parameterTypes)
        val returnType = substituteOrNull(type.returnType)
        if (parameterTypes == null && returnType == null) return null
        return ConeFunctionType(
            parameterTypes = parameterTypes ?: type.parameterTypes,
            returnType = returnType ?: type.returnType,
            isCFunc = type.isCFunc,
            isClosureType = type.isClosureType,
            hasVariableLenArg = type.hasVariableLenArg,
            attributes = type.attributes,
        )
    }

    private fun substituteTypeAlias(type: ConeTypeAliasType): ConeTypeAliasType? {
        val expandedType = type.expandedType?.let { substituteOrNull(it) ?: it }
        val typeArguments = substituteArguments(type.typeArguments)
        if (expandedType == type.expandedType && typeArguments == null) return null
        return ConeTypeAliasType(
            classId = type.classId,
            expandedType = expandedType,
            typeArguments = typeArguments ?: type.typeArguments,
            attributes = type.attributes,
        )
    }

    private fun substituteIntersectionType(type: ConeIntersectionType): ConeIntersectionType? {
        val intersectedTypes = substituteTypes(type.intersectedTypes)
        val upperBoundForApproximation = type.upperBoundForApproximation?.let { substituteOrNull(it) ?: it }
        if (intersectedTypes == null && upperBoundForApproximation == type.upperBoundForApproximation) return null

        return ConeIntersectionType(
            intersectedTypes = intersectedTypes ?: type.intersectedTypes,
            upperBoundForApproximation = upperBoundForApproximation,
            attributes = type.attributes,
        )
    }

    private fun substituteArguments(arguments: List<ConeTypeProjection>): List<ConeTypeProjection>? {
        var changed = false
        val substituted = arguments.mapIndexed { index, projection ->
            substituteArgument(projection, index)?.also { changed = true } ?: projection
        }
        return substituted.takeIf { changed }
    }

    private fun substituteTypes(types: Collection<ConeCangJieType>): List<ConeCangJieType>? {
        var changed = false
        val substituted = types.map { type ->
            substituteOrNull(type)?.also { changed = true } ?: type
        }
        return substituted.takeIf { changed }
    }
}
