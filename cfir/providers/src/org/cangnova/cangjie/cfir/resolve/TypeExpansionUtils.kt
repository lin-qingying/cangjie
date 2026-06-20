package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.util.expandedConeType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.AbbreviatedTypeAttribute
import org.cangnova.cangjie.cfir.types.AbstractConeSubstitutor
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.forEachType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.types.withAbbreviation
import org.cangnova.cangjie.cfir.types.withAttributes

context(sessionHolder: SessionHolder)
fun ConeCangJieType.fullyExpandedType(): ConeCangJieType {
    return fullyExpandedType(sessionHolder.session, expandedConeType = CfirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

fun CfirTypeAlias.expandedConeTypeWithEnsuredPhase(): ConeCangJieType? {
    lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
    return expandedConeType
}

/**
 * @see fullyExpandedType (the first function in the file)
 * @return the expanded type or the same instance if top-level constructor is not expandable type alias
 */
fun ConeClassifierType.fullyExpandedType(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeCangJieType? = CfirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeClassifierType {
    return (this as ConeCangJieType).fullyExpandedType(useSiteSession, expandedConeType) as? ConeClassifierType ?: this
}

/**
 * @see fullyExpandedType (the first function in the file)
 * @return the expanded type or the same instance if top-level constructor is not expandable type alias
 */
fun ConeCangJieType.fullyExpandedType(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeCangJieType? = CfirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeCangJieType = when (this) {
    is ConeLookupTagBasedType -> fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
    is ConeTypeAliasType -> fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
    else -> this
}

private fun ConeCangJieType.fullyExpandedTypeNoCache(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeCangJieType?,
): ConeCangJieType {
    val directExpansionType = directExpansionType(useSiteSession, expandedConeType) ?: return this
    val expansion = directExpansionType.fullyExpandedType(useSiteSession, expandedConeType)
    return expansion.withAbbreviation(AbbreviatedTypeAttribute(this))
}

fun ConeCangJieType.directExpansionType(
    useSiteSession: CfirSession,
    expandedConeType: (CfirTypeAlias) -> ConeCangJieType? = { alias ->
        alias.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
        alias.expandedConeType
    },
): ConeCangJieType? {
    val classId = when (this) {
        is ConeTypeAliasType -> classId
        is ConeLookupTagBasedType -> classIdOrPrimitiveClassId
        else -> null
    } ?: return null
    val typeAlias = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirTypeAlias

    val resultType = (this as? ConeTypeAliasType)?.expandedType
        ?: typeAlias?.let(expandedConeType)
        ?: typeAlias?.expandedTypeRef?.coneTypeOrNull
        ?: return null
    val appliedAttributes = resultType.applyAttributesFrom(this)

    if (typeAlias == null || typeArguments.isEmpty()) {
        return appliedAttributes
    }
    return mapTypeAliasArguments(typeAlias, this, appliedAttributes, useSiteSession)
}

private fun ConeCangJieType.applyAttributesFrom(
    abbreviation: ConeCangJieType,
): ConeCangJieType {
    val combinedAttributes = attributes.add(abbreviation.attributes)
    return withAttributes(combinedAttributes)
}

fun CfirTypeAlias.mapParametersToArgumentsOf(type: ConeCangJieType): List<Pair<CfirTypeParameterSymbol, ConeTypeProjection>> =
    typeParameters.map { it.symbol }.zip(type.typeArguments)

fun createParametersSubstitutor(
    useSiteSession: CfirSession,
    typeAliasMap: Map<CfirTypeParameterSymbol, ConeTypeProjection>,
): ConeSubstitutor = object : AbstractConeSubstitutor(useSiteSession.typeContext) {
    override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
        if (type !is ConeTypeParameterType) {
            return null
        }
        val mappedProjection = typeAliasMap[type.lookupTag.typeParameterSymbol] ?: return null
        val mappedType = mappedProjection.type
        return mappedType.withAttributes(type.attributes.add(mappedType.attributes))
    }
}

fun CfirTypeAlias.createParametersSubstitutor(
    abbreviatedType: ConeCangJieType,
    useSiteSession: CfirSession,
): ConeSubstitutor = createParametersSubstitutor(useSiteSession, mapParametersToArgumentsOf(abbreviatedType).toMap())

private fun mapTypeAliasArguments(
    typeAlias: CfirTypeAlias,
    abbreviatedType: ConeCangJieType,
    resultingType: ConeCangJieType,
    useSiteSession: CfirSession,
): ConeCangJieType {
    if (typeAlias.typeParameters.isNotEmpty() && abbreviatedType.typeArguments.isEmpty()) {
        return resultingType
    }

    return typeAlias.createParametersSubstitutor(abbreviatedType, useSiteSession).substituteOrSelf(resultingType)
}

fun CfirTypeAlias.fullyExpandedConeType(useSiteSession: CfirSession): ConeCangJieType? {
    return expandedConeType?.fullyExpandedType(useSiteSession)
}

inline fun ConeCangJieType.forEachExpandedType(
    session: CfirSession,
    action: (ConeCangJieType) -> Unit,
) {
    forEachType(
        prepareType = { type ->
            if (type is ConeTypeAliasType) {
                type.fullyExpandedType(session)
            } else {
                type
            }
        },
        action = action,
    )
}
