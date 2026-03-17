package org.cangnova.cangjie.cfir.types

fun ConeCangjieType.renderSemanticKey(): String = when (this) {
    is ConePrimitiveType -> "primitive:${kind.name}"
    is ConeClassLikeType -> "class:${classId.asString()}<${typeArguments.renderTypeArgsKey()}>:this=$isThisType"
    is ConeStructType -> "struct:${classId.asString()}<${typeArguments.renderTypeArgsKey()}>"
    is ConeEnumType -> "enum:${classId.asString()}<${typeArguments.renderTypeArgsKey()}>:ref=$isRefEnum"
    is ConeTypeAliasType -> "typealias:${classId.asString()}<${typeArguments.renderTypeArgsKey()}>"
    is ConeTypeParameterType -> "typeparam:${lookupTag.name}"
    is ConeFuncType -> "func:c=$isCFunc:closure=$isClosureType:vararg=$hasVariableLenArg:params=[${parameterTypes.renderTypeArgsKey()}]:ret=${returnType.renderSemanticKey()}"
    is ConeTupleType -> "tuple:[${elementTypes.renderTypeArgsKey()}]"
    is ConeArrayType -> "array:dims=$dims:${elementType.renderSemanticKey()}"
    is ConeVArrayType -> "varray:size=$size:${elementType.renderSemanticKey()}"
    is ConePointerType -> "pointer:${pointeeType.renderSemanticKey()}"
    is ConeCStringType -> "cstring"
    is ConeIntersectionType -> "intersection:[${intersectedTypes.map { it.renderSemanticKey() }.sorted().joinToString(",")}]"
    is ConeUnionType -> "union:[${unionTypes.map { it.renderSemanticKey() }.sorted().joinToString(",")}]"
    is ConeFlexibleType -> "flexible:${lowerBound.renderSemanticKey()}..${upperBound.renderSemanticKey()}"
    is ConeQuestType -> "quest"
    is ConeErrorType -> "cone-error:$reason"
    ConeAnyType -> "any"
}

private fun List<ConeCangjieType>.renderTypeArgsKey(): String = joinToString(",") { it.renderSemanticKey() }
