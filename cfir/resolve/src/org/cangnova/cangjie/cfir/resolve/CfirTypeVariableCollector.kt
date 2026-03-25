package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType

import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType

internal fun ConeCangJieType.collectTypeVariableNames(result: MutableSet<String>) {
    when (this) {
        is ConeTypeParameterType -> result += lookupTag.name.asString()
        is ConeClassLikeType -> typeArguments.forEach { it.collectTypeVariableNames(result) }
        is ConeStructType -> typeArguments.forEach { it.collectTypeVariableNames(result) }
        is ConeEnumType -> typeArguments.forEach { it.collectTypeVariableNames(result) }
        is ConeFuncType -> {
            parameterTypes.forEach { it.collectTypeVariableNames(result) }
            returnType.collectTypeVariableNames(result)
        }
        is ConeTupleType -> elementTypes.forEach { it.collectTypeVariableNames(result) }
        is ConeVArrayType -> elementType.collectTypeVariableNames(result)
        is ConePointerType -> pointeeType.collectTypeVariableNames(result)
        is ConeTypeAliasType -> {
            expandedType?.collectTypeVariableNames(result)
            typeArguments.forEach { it.collectTypeVariableNames(result) }
        }
        is ConeIntersectionType -> intersectedTypes.forEach { it.collectTypeVariableNames(result) }
        is ConeUnionType -> unionTypes.forEach { it.collectTypeVariableNames(result) }
        else -> arrayElementType?.collectTypeVariableNames(result)
    }
}

private fun ConeTypeProjection.collectTypeVariableNames(result: MutableSet<String>) {
    type.collectTypeVariableNames(result)
}
