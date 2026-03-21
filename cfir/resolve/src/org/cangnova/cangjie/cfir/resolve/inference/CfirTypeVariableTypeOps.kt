package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.*

internal fun ConeCangJieType.containsTypeVariableName(variableName: String): Boolean {
    return when (this) {
        is ConeTypeParameterType -> name == variableName
        is ConeClassLikeType -> typeArguments.any { it.containsTypeVariableName(variableName) }
        is ConeStructType -> typeArguments.any { it.containsTypeVariableName(variableName) }
        is ConeEnumType -> typeArguments.any { it.containsTypeVariableName(variableName) }
        is ConeFuncType -> parameterTypes.any { it.containsTypeVariableName(variableName) } ||
            returnType.containsTypeVariableName(variableName)
        is ConeTupleType -> elementTypes.any { it.containsTypeVariableName(variableName) }
        is ConeArrayType -> elementType.containsTypeVariableName(variableName)
        is ConeVArrayType -> elementType.containsTypeVariableName(variableName)
        is ConeIntersectionType -> intersectedTypes.any { it.containsTypeVariableName(variableName) }
        is ConeUnionType -> unionTypes.any { it.containsTypeVariableName(variableName) }
        else -> false
    }
}

internal fun ConeCangJieType.collectTypeVariableNames(into: MutableSet<String>) {
    when (this) {
        is ConeTypeParameterType -> into += name
        is ConeClassLikeType -> typeArguments.forEach { it.collectTypeVariableNames(into) }
        is ConeStructType -> typeArguments.forEach { it.collectTypeVariableNames(into) }
        is ConeEnumType -> typeArguments.forEach { it.collectTypeVariableNames(into) }
        is ConeFuncType -> {
            parameterTypes.forEach { it.collectTypeVariableNames(into) }
            returnType.collectTypeVariableNames(into)
        }
        is ConeTupleType -> elementTypes.forEach { it.collectTypeVariableNames(into) }
        is ConeArrayType -> elementType.collectTypeVariableNames(into)
        is ConeVArrayType -> elementType.collectTypeVariableNames(into)
        is ConeIntersectionType -> intersectedTypes.forEach { it.collectTypeVariableNames(into) }
        is ConeUnionType -> unionTypes.forEach { it.collectTypeVariableNames(into) }

        else -> Unit
    }
}

internal fun ConeCangJieType.substituteTypeVariableName(
    variableName: String,
    replacement: ConeCangJieType,
): ConeCangJieType {
    return when (this) {
        is ConeTypeParameterType -> if (name == variableName) replacement else this
        is ConeClassLikeType -> {
            val newArgs = typeArguments.map { it.substituteTypeVariableName(variableName, replacement) }
            if (newArgs == typeArguments) this
            else ConeClassLikeType(lookupTag, newArgs, attributes, isInterface, isThisType)
        }
        is ConeStructType -> {
            val newArgs = typeArguments.map { it.substituteTypeVariableName(variableName, replacement) }
            if (newArgs == typeArguments) this else ConeStructType(lookupTag, newArgs, attributes)
        }
        is ConeEnumType -> {
            val newArgs = typeArguments.map { it.substituteTypeVariableName(variableName, replacement) }
            if (newArgs == typeArguments) this else ConeEnumType(lookupTag, newArgs, attributes, isRefEnum)
        }
        is ConeFuncType -> {
            val newParams = parameterTypes.map { it.substituteTypeVariableName(variableName, replacement) }
            val newReturn = returnType.substituteTypeVariableName(variableName, replacement)
            if (newParams == parameterTypes && newReturn == returnType) this
            else ConeFuncType(newParams, newReturn, isCFunc, isClosureType, hasVariableLenArg, attributes)
        }
        is ConeTupleType -> {
            val newElements = elementTypes.map { it.substituteTypeVariableName(variableName, replacement) }
            if (newElements == elementTypes) this else ConeTupleType(newElements, attributes)
        }
        is ConeArrayType -> {
            val newElement = elementType.substituteTypeVariableName(variableName, replacement)
            if (newElement == elementType) this else ConeArrayType(newElement, dims, attributes)
        }
        is ConeVArrayType -> {
            val newElement = elementType.substituteTypeVariableName(variableName, replacement)
            if (newElement == elementType) this else ConeVArrayType(newElement, size, attributes)
        }
        is ConeIntersectionType -> {
            val newTypes = intersectedTypes.map { it.substituteTypeVariableName(variableName, replacement) }
            if (newTypes == intersectedTypes) this else ConeIntersectionType(newTypes)
        }
        is ConeUnionType -> {
            val newTypes = unionTypes.map { it.substituteTypeVariableName(variableName, replacement) }.toSet()
            if (newTypes == unionTypes) this else ConeUnionType(newTypes)
        }

        else -> this
    }
}
