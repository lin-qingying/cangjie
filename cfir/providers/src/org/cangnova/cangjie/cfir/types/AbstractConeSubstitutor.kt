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

/**
 * 基于 cone type 结构递归的抽象替换器。
 *
 * 子类只需要实现单个类型头的替换规则，[AbstractConeSubstitutor] 会继续递归替换
 * 类型参数、结构类型组成部分和类型 attributes。
 */
abstract class AbstractConeSubstitutor(
    /**
     * 替换过程中使用的类型上下文。
     */
    protected val typeContext: ConeTypeContext,
) : ConeSubstitutor() {
    /**
     * 尝试替换 [type] 本身。
     *
     * 返回 `null` 表示当前类型头没有直接替换结果，需要继续递归进入结构字段。
     */
    abstract fun substituteType(type: ConeCangJieType): ConeCangJieType?

    /**
     * 替换类型实参中的类型。
     */
    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val newType = substituteOrNull(projection.type) ?: return null
        return newType
    }

    /**
     * 替换 [type] 或其内部结构；返回 `null` 表示完全没有变化。
     */
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

    /**
     * 按具体 cone type 形态递归进入结构字段。
     */
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

    /**
     * 替换错误类型携带的委托类型和类型实参。
     */
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

    /**
     * 替换 typealias 的 expanded type 与类型实参。
     */
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

    /**
     * 替换元组元素类型。
     */
    private fun ConeTupleType.substituteTupleType(): ConeCangJieType? {
        val substitutedElements = substituteTypes(elementTypes) ?: return null
        return ConeTupleType(substitutedElements, attributes)
    }

    /**
     * 替换 VArray 元素类型。
     */
    private fun ConeVArrayType.substituteVArrayType(): ConeCangJieType? {
        val substitutedElementType = substituteOrNull(elementType) ?: return null
        return ConeVArrayType(substitutedElementType, size, attributes)
    }

    /**
     * 替换指针指向类型。
     */
    private fun ConePointerType.substitutePointerType(): ConeCangJieType? {
        val substitutedPointeeType = substituteOrNull(pointeeType) ?: return null
        return ConePointerType(substitutedPointeeType, attributes)
    }

    /**
     * 替换交叉类型的组成项和近似上界。
     */
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

    /**
     * 替换联合类型的组成项。
     */
    private fun ConeUnionType.substituteUnionType(): ConeCangJieType? {
        val substitutedUnionTypes = substituteTypes(unionTypes) ?: return null
        return ConeUnionType(substitutedUnionTypes.toSet(), attributes)
    }

    /**
     * 替换类型集合；只有至少一个元素变化时返回新列表。
     */
    private fun substituteTypes(types: Collection<ConeCangJieType>): List<ConeCangJieType>? {
        var changed = false
        val substituted = types.map { type ->
            substituteOrNull(type)?.also { changed = true } ?: type
        }
        return substituted.takeIf { changed }
    }

    /**
     * 替换 rigid type 的类型实参。
     */
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

/**
 * 根据类型构造器映射创建替换器。
 */
fun createTypeSubstitutorByTypeConstructor(
    map: Map<TypeConstructorMarker, ConeCangJieType>,
    context: ConeTypeContext,
    approximateIntegerLiterals: Boolean,
): ConeSubstitutor {
    if (map.isEmpty()) return ConeSubstitutor.Empty
    return ConeTypeSubstitutorByTypeConstructor(map, context, approximateIntegerLiterals)
}

/**
 * 以类型构造器身份为 key 的 concrete cone 替换器。
 */
private class ConeTypeSubstitutorByTypeConstructor(
    /**
     * 类型构造器到替换目标类型的映射。
     */
    private val map: Map<TypeConstructorMarker, ConeCangJieType>,
    typeContext: ConeTypeContext,
    /**
     * 是否在替换结果中把 integer literal 类型近似为具体 primitive。
     */
    private val approximateIntegerLiterals: Boolean,
) : AbstractConeSubstitutor(typeContext) {

    /**
     * 根据 [type] 的可替换构造器查表得到替换类型。
     */
    override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
        val constructor = type.typeConstructorForSubstitution() ?: return null
        val newType = map[constructor] ?: return null
        return if (approximateIntegerLiterals) newType.approximateIntegerLiteralType() else newType
    }

    /**
     * 返回替换映射的调试文本。
     */
    override fun toString(): String {
        return map.entries.joinToString(prefix = "{", postfix = "}", separator = " | ") { (constructor, type) ->
            "$constructor -> $type"
        }
    }
}

/**
 * 不执行任何替换的空 substitutor。
 */
object ConeEmptySubstitutor : ConeSubstitutor() {
    /**
     * 空替换器始终返回原类型。
     */
    override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = type

    /**
     * 空替换器没有可空替换结果。
     */
    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? = null

    /**
     * 空替换器不替换类型实参。
     */
    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? = null

    /**
     * 返回空替换器调试文本。
     */
    override fun toString(): String = "ConeEmptySubstitutor"
}

/**
 * 将通用类型系统 substitutor marker 还原为 cone substitutor。
 */
@Suppress("NOTHING_TO_INLINE")
inline fun TypeSubstitutorMarker.asCone(): ConeSubstitutor = this as ConeSubstitutor

/**
 * 保留给旧调用点的冗余转换函数。
 */
@Deprecated(message = "This call is redundant, please just drop it", level = DeprecationLevel.ERROR)
fun ConeSubstitutor.asCone(): ConeSubstitutor = this

/**
 * 对可空类型执行替换。
 */
fun ConeSubstitutor.substituteOrNull(type: ConeCangJieType?): ConeCangJieType? {
    return type?.let { substituteOrNull(it) }
}

/**
 * 返回当前类型用于替换查表的类型构造器。
 */
private fun ConeCangJieType.typeConstructorForSubstitution(): TypeConstructorMarker? {
    return when (this) {
        is ConeLookupTagBasedType -> lookupTag
        is ConeTypeVariableType -> typeConstructor
        is ConeStubType -> constructor
        is ConeTypeConstructorMarker -> this
        else -> null
    }
}

/**
 * 必要时把 ideal/integer literal 类型近似为具体 primitive 类型。
 */
private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType {
    return when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        is ConePrimitiveType -> IdealTypeResolver.resolveIfIdeal(this)
        else -> this
    }
}
