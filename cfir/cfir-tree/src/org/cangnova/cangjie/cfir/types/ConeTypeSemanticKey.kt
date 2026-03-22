package org.cangnova.cangjie.cfir.types

fun ConeCangJieType.renderSemanticKey(): String = when (this) {
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
    is ConeQuestType -> "quest"
    is ConeErrorType -> "cone-error:$reason"
    is ConePlaceholderType -> "placeholder:${id.name}#${id.freshId}"
    is ConeDeferredType -> "deferred:${id.name}#${id.freshId}"
    is ConeTypeVariableRef -> "typevar-ref:${state.id.name}#${state.id.freshId}"
    is ConeCapturedType -> "captured:${constructor.name}:${status.name}"
    is ConeStubType -> "stub:${constructor.name}:${kind.name}"
    is ConeTypeVariableType -> "typevar:${typeVariableConstructor.name}"
    ConeAnyType -> "any"
}

private fun List<ConeCangJieType>.renderTypeArgsKey(): String = joinToString(",") { it.renderSemanticKey() }


