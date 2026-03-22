package org.cangnova.cangjie.cfir.constraints

import org.cangnova.cangjie.cfir.types.*

interface CfirTypeSubstitutor {
    fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType

    companion object {
        val Empty: CfirTypeSubstitutor = EmptySubstitutor
    }
}

private object EmptySubstitutor : CfirTypeSubstitutor {
    override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = type
    override fun toString(): String = "CfirTypeSubstitutor.Empty"
}

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

            ConeAnyType -> type
            is ConeCStringType -> type
            is ConeCapturedType -> type
            is ConeDeferredType -> type
            is ConeErrorType -> type
            is ConePlaceholderType -> type
            is ConePrimitiveType -> type
            is ConeQuestType -> type
            is ConeStubType -> type
            is ConeTypeVariableRef -> type
            is ConeTypeVariableType -> type
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




    override fun toString(): String = "CfirTypeSubstitutorByMap($substitution)"
}
