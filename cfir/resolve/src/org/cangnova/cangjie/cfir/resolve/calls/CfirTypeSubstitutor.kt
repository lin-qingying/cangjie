package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.types.*

/**
 * 类型参数替换器接口。
 *
 * 将类型中的 ConeTypeParameterType 替换为具体类型。
 * Phase 3 仅支持显式类型参数的 Map 替换，完整类型推断留到 Phase 4。
 *
 * 对齐 K2 ConeSubstitutor / TypeSubstitutor。
 */
interface CfirTypeSubstitutor {

    /**
     * 替换类型中的类型参数。
     *
     * @param type 待替换的类型
     * @return 替换后的类型；若无需替换则返回原类型
     */
    fun substituteOrSelf(type: ConeCangjieType): ConeCangjieType

    companion object {
        /** 空替换器（不做任何替换） */
        val Empty: CfirTypeSubstitutor = EmptySubstitutor
    }
}

/** 空替换器实现 */
private object EmptySubstitutor : CfirTypeSubstitutor {
    override fun substituteOrSelf(type: ConeCangjieType): ConeCangjieType = type
    override fun toString(): String = "CfirTypeSubstitutor.Empty"
}

/**
 * 基于 Map 的类型参数替换器。
 *
 * 将 ConeTypeParameterType 按 lookupTag.name 匹配替换。
 * 对复合类型（函数类型、元组等）递归替换具体类型的类型参数。
 */
class CfirTypeSubstitutorByMap(
    private val substitution: Map<String, ConeCangjieType>,
) : CfirTypeSubstitutor {

    override fun substituteOrSelf(type: ConeCangjieType): ConeCangjieType {
        if (substitution.isEmpty()) return type
        return substituteType(type)
    }

    private fun substituteType(type: ConeCangjieType): ConeCangjieType {
        return when (type) {
            is ConeTypeParameterType -> substitution[type.lookupTag.name] ?: type
            is ConeClassLikeType -> substituteClassLikeType(type)
            is ConeStructType -> substituteStructType(type)
            is ConeEnumType -> type // 枚举类型暂不替换
            is ConeFuncType -> substituteFuncType(type)
            is ConeTupleType -> substituteTupleType(type)
            is ConeArrayType -> substituteArrayType(type)
            else -> type // 原始类型、错误类型等无需替换
        }
    }

    private fun substituteClassLikeType(type: ConeClassLikeType): ConeCangjieType {
        if (type.typeArguments.isEmpty()) return type
        val newArgs = type.typeArguments.map { substituteType(it) }
        if (newArgs == type.typeArguments) return type
        return ConeClassLikeType(type.lookupTag, newArgs, type.attributes, type.isInterface, type.isThisType)
    }

    private fun substituteStructType(type: ConeStructType): ConeCangjieType {
        if (type.typeArguments.isEmpty()) return type
        val newArgs = type.typeArguments.map { substituteType(it) }
        if (newArgs == type.typeArguments) return type
        return ConeStructType(type.lookupTag, newArgs, type.attributes)
    }

    private fun substituteFuncType(type: ConeFuncType): ConeCangjieType {
        val newParams = type.parameterTypes.map { substituteType(it) }
        val newReturn = substituteType(type.returnType)
        if (newParams == type.parameterTypes && newReturn == type.returnType) return type
        return ConeFuncType(newParams, newReturn, type.isCFunc, type.isClosureType, type.hasVariableLenArg)
    }

    private fun substituteTupleType(type: ConeTupleType): ConeCangjieType {
        val newElements = type.elementTypes.map { substituteType(it) }
        if (newElements == type.elementTypes) return type
        return ConeTupleType(newElements, type.attributes)
    }

    private fun substituteArrayType(type: ConeArrayType): ConeCangjieType {
        val newElement = substituteType(type.elementType)
        if (newElement == type.elementType) return type
        return ConeArrayType(newElement, type.dims)
    }

    override fun toString(): String = "CfirTypeSubstitutorByMap($substitution)"
}
