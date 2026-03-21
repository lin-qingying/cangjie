package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.types.*

/**
 * 类型参数替换器接口。
 * 用于把类型中的 `ConeTypeParameterType` 替换为具体类型。
 * Phase 3 主要支持显式类型实参驱动的 map 替换；更完整的推断留到后续阶段。
 * 对齐 K2 `ConeSubstitutor` / `TypeSubstitutor`。
 */
interface CfirTypeSubstitutor {

    /**
     * 替换类型中的类型参数。
     * @param type 待替换的类型
     * @return 替换后的类型；若无需替换则返回原类型
     */
    fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType

    companion object {
        /** 空替换器，不做任何替换。 */
        val Empty: CfirTypeSubstitutor = EmptySubstitutor
    }
}

/** 空替换器实现。 */
private object EmptySubstitutor : CfirTypeSubstitutor {
    override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = type
    override fun toString(): String = "CfirTypeSubstitutor.Empty"
}

/**
 * 基于 `Map` 的类型参数替换器。
 * 它按 `lookupTag.name` 匹配 `ConeTypeParameterType`，
 * 并递归替换复合类型中的各类类型参数。
 */
class CfirTypeSubstitutorByMap(
    private val substitution: Map<String, ConeCangJieType>,
) : CfirTypeSubstitutor {

    override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType {
        if (substitution.isEmpty()) return type
        return substituteType(type)
    }

    private fun substituteType(type: ConeCangJieType): ConeCangJieType {
        return when (type) {
            is ConeTypeParameterType -> substitution[type.lookupTag.name] ?: type
            is ConeClassLikeType -> substituteClassLikeType(type)
            is ConeStructType -> substituteStructType(type)
            is ConeEnumType -> substituteEnumType(type)
            is ConeFuncType -> substituteFuncType(type)
            is ConeTupleType -> substituteTupleType(type)
            is ConeArrayType -> substituteArrayType(type)
            is ConeVArrayType -> substituteVArrayType(type)
            is ConePointerType -> substitutePointerType(type)
            is ConeTypeAliasType -> substituteTypeAliasType(type)
            is ConeIntersectionType -> substituteIntersectionType(type)
            is ConeUnionType -> substituteUnionType(type)
            is ConeFlexibleType -> substituteFlexibleType(type)
            else -> type // 原始类型、错误类型等无需替换
        }
    }

    private fun substituteClassLikeType(type: ConeClassLikeType): ConeCangJieType {
        if (type.typeArguments.isEmpty()) return type
        val newArgs = type.typeArguments.map { substituteType(it) }
        if (newArgs == type.typeArguments) return type
        return ConeClassLikeType(type.lookupTag, newArgs, type.attributes, type.isInterface, type.isThisType)
    }

    private fun substituteStructType(type: ConeStructType): ConeCangJieType {
        if (type.typeArguments.isEmpty()) return type
        val newArgs = type.typeArguments.map { substituteType(it) }
        if (newArgs == type.typeArguments) return type
        return ConeStructType(type.lookupTag, newArgs, type.attributes)
    }

    private fun substituteEnumType(type: ConeEnumType): ConeCangJieType {
        if (type.typeArguments.isEmpty()) return type
        val newArgs = type.typeArguments.map { substituteType(it) }
        if (newArgs == type.typeArguments) return type
        return ConeEnumType(type.lookupTag, newArgs, type.attributes, type.isRefEnum)
    }

    private fun substituteFuncType(type: ConeFuncType): ConeCangJieType {
        val newParams = type.parameterTypes.map { substituteType(it) }
        val newReturn = substituteType(type.returnType)
        if (newParams == type.parameterTypes && newReturn == type.returnType) return type
        return ConeFuncType(newParams, newReturn, type.isCFunc, type.isClosureType, type.hasVariableLenArg)
    }

    private fun substituteTupleType(type: ConeTupleType): ConeCangJieType {
        val newElements = type.elementTypes.map { substituteType(it) }
        if (newElements == type.elementTypes) return type
        return ConeTupleType(newElements, type.attributes)
    }

    private fun substituteArrayType(type: ConeArrayType): ConeCangJieType {
        val newElement = substituteType(type.elementType)
        if (newElement == type.elementType) return type
        return ConeArrayType(newElement, type.dims)
    }

    private fun substituteVArrayType(type: ConeVArrayType): ConeCangJieType {
        val newElement = substituteType(type.elementType)
        if (newElement == type.elementType) return type
        return ConeVArrayType(newElement, type.size, type.attributes)
    }

    private fun substitutePointerType(type: ConePointerType): ConeCangJieType {
        val newPointee = substituteType(type.pointeeType)
        if (newPointee == type.pointeeType) return type
        return ConePointerType(newPointee, type.attributes)
    }

    private fun substituteTypeAliasType(type: ConeTypeAliasType): ConeCangJieType {
        val newExpanded = type.expandedType?.let { substituteType(it) }
        val newArgs = type.typeArguments.map { substituteType(it) }
        if (newExpanded == type.expandedType && newArgs == type.typeArguments) return type
        return ConeTypeAliasType(type.classId, newExpanded, newArgs, type.attributes)
    }

    private fun substituteIntersectionType(type: ConeIntersectionType): ConeCangJieType {
        val newTypes = type.intersectedTypes.map { substituteType(it) }
        if (newTypes == type.intersectedTypes) return type
        return ConeIntersectionType(newTypes)
    }

    private fun substituteUnionType(type: ConeUnionType): ConeCangJieType {
        val newTypes = type.unionTypes.map { substituteType(it) }.toSet()
        if (newTypes == type.unionTypes) return type
        return ConeUnionType(newTypes)
    }

    private fun substituteFlexibleType(type: ConeFlexibleType): ConeCangJieType {
        val newLower = substituteType(type.lowerBound)
        val newUpper = substituteType(type.upperBound)
        if (newLower == type.lowerBound && newUpper == type.upperBound) return type
        if (newLower !is ConeRigidType || newUpper !is ConeRigidType) return type
        return ConeFlexibleType(newLower, newUpper)
    }

    override fun toString(): String = "CfirTypeSubstitutorByMap($substitution)"
}

