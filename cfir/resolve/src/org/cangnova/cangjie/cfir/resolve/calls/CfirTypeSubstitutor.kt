package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.types.*

/**
 * 绫诲瀷鍙傛暟鏇挎崲鍣ㄦ帴鍙ｃ€? *
 * 灏嗙被鍨嬩腑鐨?ConeTypeParameterType 鏇挎崲涓哄叿浣撶被鍨嬨€? * Phase 3 浠呮敮鎸佹樉寮忕被鍨嬪弬鏁扮殑 Map 鏇挎崲锛屽畬鏁寸被鍨嬫帹鏂暀鍒?Phase 4銆? *
 * 瀵归綈 K2 ConeSubstitutor / TypeSubstitutor銆? */
interface CfirTypeSubstitutor {

    /**
     * 鏇挎崲绫诲瀷涓殑绫诲瀷鍙傛暟銆?     *
     * @param type 寰呮浛鎹㈢殑绫诲瀷
     * @return 鏇挎崲鍚庣殑绫诲瀷锛涜嫢鏃犻渶鏇挎崲鍒欒繑鍥炲師绫诲瀷
     */
    fun substituteOrSelf(type: ConeCangjieType): ConeCangjieType

    companion object {
        /** 绌烘浛鎹㈠櫒锛堜笉鍋氫换浣曟浛鎹級 */
        val Empty: CfirTypeSubstitutor = EmptySubstitutor
    }
}

/** 绌烘浛鎹㈠櫒瀹炵幇 */
private object EmptySubstitutor : CfirTypeSubstitutor {
    override fun substituteOrSelf(type: ConeCangjieType): ConeCangjieType = type
    override fun toString(): String = "CfirTypeSubstitutor.Empty"
}

/**
 * 鍩轰簬 Map 鐨勭被鍨嬪弬鏁版浛鎹㈠櫒銆? *
 * 灏?ConeTypeParameterType 鎸?lookupTag.name 鍖归厤鏇挎崲銆? * 瀵瑰鍚堢被鍨嬶紙鍑芥暟绫诲瀷銆佸厓缁勭瓑锛夐€掑綊鏇挎崲鍏蜂綋绫诲瀷鐨勭被鍨嬪弬鏁般€? */
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
            is ConeEnumType -> type // 鏋氫妇绫诲瀷鏆備笉鏇挎崲
            is ConeFuncType -> substituteFuncType(type)
            is ConeTupleType -> substituteTupleType(type)
            is ConeArrayType -> substituteArrayType(type)
            else -> type // 鍘熷绫诲瀷銆侀敊璇被鍨嬬瓑鏃犻渶鏇挎崲
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

