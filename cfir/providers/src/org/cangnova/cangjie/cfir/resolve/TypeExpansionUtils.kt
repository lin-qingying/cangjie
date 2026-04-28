package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.util.expandedConeType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.type

context(sessionHolder: SessionHolder)
fun ConeCangJieType.fullyExpandedType(): ConeCangJieType {
    return fullyExpandedType(sessionHolder.session, expandedConeType = CfirTypeAlias::expandedConeTypeWithEnsuredPhase)
}
fun CfirTypeAlias.expandedConeTypeWithEnsuredPhase(): ConeClassLikeType? {
    lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
    return expandedConeType
}
/**
 * @see fullyExpandedType (the first function in the file)
 * @return the expanded type or the same instance if top-level constructor is not expandable type alias
 */
fun ConeClassifierType.fullyExpandedType(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeClassLikeType? = CfirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeClassifierType = when (this) {
    is ConeClassLikeType -> this
    else -> this
}


/**
 * @see fullyExpandedType (the first function in the file)
 * @return the expanded type or the same instance if top-level constructor is not expandable type alias
 */
fun ConeCangJieType.fullyExpandedType(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeClassLikeType? = CfirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeCangJieType = when (this) {
    is ConeTypeAliasType -> fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
    else -> this
}

private fun ConeTypeAliasType.fullyExpandedTypeNoCache(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeClassLikeType?,
): ConeCangJieType {
    val directExpansionType = directExpansionType(useSiteSession, expandedConeType) ?: return this
    return directExpansionType.fullyExpandedType(useSiteSession, expandedConeType)
}

private fun ConeTypeAliasType.directExpansionType(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeClassLikeType?,
): ConeCangJieType? {
    val typeAlias = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirTypeAlias

    val resolvedExpansion = expandedType
        ?: typeAlias?.let(expandedConeType)
        ?: typeAlias?.expandedTypeRef?.coneTypeOrNull
        ?: return null

    val substitutedExpansion = substituteTypeAliasArguments(typeAlias, resolvedExpansion)
    return substitutedExpansion.withAttributes(substitutedExpansion.attributes.add(attributes))
}

private fun ConeTypeAliasType.substituteTypeAliasArguments(
    typeAlias: CfirTypeAlias?,
    resolvedExpansion: ConeCangJieType,
): ConeCangJieType {
    val typeParameters = typeAlias?.typeParameters.orEmpty()
    if (typeArguments.isEmpty() || typeParameters.isEmpty()) {
        return resolvedExpansion
    }

    val replacements = buildMap {
        for ((index, typeParameter) in typeParameters.withIndex()) {
            val typeArgument = typeArguments.getOrNull(index)?.type ?: continue
            put(typeParameter.symbol.name.asString(), typeArgument)
        }
    }
    if (replacements.isEmpty()) {
        return resolvedExpansion
    }

    return resolvedExpansion.substituteTypeParameters(replacements)
}

private fun ConeCangJieType.substituteTypeParameters(
    replacements: Map<String, ConeCangJieType>,
): ConeCangJieType {
    return when (this) {
        is ConeTypeParameterType -> replacements[lookupTag.name.asString()] ?: this
        is ConeClassLikeType -> substituteArguments(replacements)?.let { arguments ->
            ConeClassLikeType(lookupTag, arguments, attributes, isInterface, isThisType)
        } ?: this

        is ConeStructType -> substituteArguments(replacements)?.let { arguments ->
            ConeStructType(lookupTag, arguments, attributes)
        } ?: this

        is ConeEnumType -> substituteArguments(replacements)?.let { arguments ->
            ConeEnumType(lookupTag, arguments, attributes, isRefEnum)
        } ?: this

        is ConeFunctionType -> {
            val parameterTypes = parameterTypes.substituteTypes(replacements) ?: this.parameterTypes
            val returnType = returnType.substituteTypeParameters(replacements)
            if (parameterTypes === this.parameterTypes && returnType === this.returnType) {
                this
            } else {
                ConeFunctionType(parameterTypes, returnType, isCFunc, isClosureType, hasVariableLenArg, attributes)
            }
        }

        is ConeTupleType -> elementTypes.substituteTypes(replacements)?.let { elements ->
            ConeTupleType(elements, attributes)
        } ?: this

        is ConeVArrayType -> {
            val substitutedElementType = elementType.substituteTypeParameters(replacements)
            if (substitutedElementType === elementType) this else ConeVArrayType(substitutedElementType, size, attributes)
        }

        is ConePointerType -> {
            val substitutedPointeeType = pointeeType.substituteTypeParameters(replacements)
            if (substitutedPointeeType === pointeeType) this else ConePointerType(substitutedPointeeType, attributes)
        }

        is ConeTypeAliasType -> {
            val substitutedExpandedType = expandedType?.substituteTypeParameters(replacements)
            val substitutedArguments = substituteArguments(replacements)
            if (substitutedExpandedType === expandedType && substitutedArguments == null) {
                this
            } else {
                ConeTypeAliasType(
                    classId = classId,
                    expandedType = substitutedExpandedType ?: expandedType,
                    typeArguments = substitutedArguments ?: typeArguments,
                    attributes = attributes,
                )
            }
        }

        is ConeIntersectionType -> intersectedTypes.substituteTypes(replacements)?.let { intersected ->
            ConeIntersectionType(intersected, attributes)
        } ?: this

        is ConeUnionType -> {
            val substitutedUnionTypes = unionTypes.toList().substituteTypes(replacements)
            if (substitutedUnionTypes == null) this else ConeUnionType(substitutedUnionTypes.toSet(), attributes)
        }

        is ConeErrorType -> {
            val substitutedDelegatedType = delegatedType?.substituteTypeParameters(replacements)
            val substitutedArguments = substituteArguments(replacements)
            if (substitutedDelegatedType === delegatedType && substitutedArguments == null) {
                this
            } else {
                ConeErrorType(
                    diagnostic = diagnostic,
                    isUninferredParameter = isUninferredParameter,
                    delegatedType = substitutedDelegatedType,
                    typeArguments = substitutedArguments ?: typeArguments,
                    attributes = attributes,
                )
            }
        }

        else -> this
    }
}

private fun ConeCangJieType.substituteArguments(
    replacements: Map<String, ConeCangJieType>,
): List<ConeTypeProjection>? {
    if (typeArguments.isEmpty()) {
        return null
    }

    var changed = false
    val substitutedArguments = typeArguments.map { projection ->
        val substitutedType = projection.type.substituteTypeParameters(replacements)
        if (substitutedType !== projection.type) {
            changed = true
            substitutedType
        } else {
            projection
        }
    }
    return substitutedArguments.takeIf { changed }
}

private fun List<ConeCangJieType>.substituteTypes(
    replacements: Map<String, ConeCangJieType>,
): List<ConeCangJieType>? {
    var changed = false
    val substitutedTypes = map { type ->
        val substitutedType = type.substituteTypeParameters(replacements)
        if (substitutedType !== type) {
            changed = true
        }
        substitutedType
    }
    return substitutedTypes.takeIf { changed }
}

private fun ConeCangJieType.withAttributes(newAttributes: ConeAttributes): ConeCangJieType {
    return when (this) {
        is ConeClassLikeType -> ConeClassLikeType(lookupTag, typeArguments, newAttributes, isInterface, isThisType)
        is ConeStructType -> ConeStructType(lookupTag, typeArguments, newAttributes)
        is ConeEnumType -> ConeEnumType(lookupTag, typeArguments, newAttributes, isRefEnum)
        is ConePrimitiveType -> ConePrimitiveType(kind, newAttributes)
        is ConeCStringType -> ConeCStringType(newAttributes)
        is ConeTypeParameterType -> ConeTypeParameterTypeImpl(lookupTag, newAttributes)
        is ConeFunctionType -> ConeFunctionType(parameterTypes, returnType, isCFunc, isClosureType, hasVariableLenArg, newAttributes)
        is ConeTupleType -> ConeTupleType(elementTypes, newAttributes)
        is ConeVArrayType -> ConeVArrayType(elementType, size, newAttributes)
        is ConePointerType -> ConePointerType(pointeeType, newAttributes)
        is ConeIntersectionType -> ConeIntersectionType(intersectedTypes, newAttributes)
        is ConeUnionType -> ConeUnionType(unionTypes, newAttributes)
        is ConeTypeAliasType -> ConeTypeAliasType(classId, expandedType, typeArguments, newAttributes)
        is ConeErrorType -> ConeErrorType(
            diagnostic,
            isUninferredParameter,
            delegatedType,
            typeArguments,
            newAttributes
        )
        is ConeQuestType -> ConeQuestType(newAttributes)
        is ConeTypeVariableType -> ConeTypeVariableType(typeConstructor, newAttributes)
        is ConePlaceholderType -> ConePlaceholderType(debugName, newAttributes)
        else -> this
    }
}
