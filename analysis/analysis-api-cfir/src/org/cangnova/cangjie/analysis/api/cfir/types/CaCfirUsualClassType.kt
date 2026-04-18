package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asPublicTypeProjections
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreUsualClassType
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaResolvedClassTypeQualifier
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.name.ClassId

/**
 * 仓颉常规 class-like public type。
 *
 * 对类型别名场景，这里保留别名本身作为 public type；
 * `fullyExpandedType` 仍由 type component 侧负责展开。
 */
internal class CaCfirUsualClassType(
    override val coneType: ConeCangJieType,
    override val analysisSession: CaCfirSession,
) : CaUsualClassType(), CaCfirType {
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: CaUsualClassType?
        get() = withValidityAssertion { this.takeIf { coneType is ConeTypeAliasType } }

    override val classId: ClassId
        get() = withValidityAssertion {
            coneType.classIdOrPrimitiveClassId
                ?: error("Only class-like CFIR types can expose ClassId: ${coneType::class.simpleName}")
        }

    override val symbol: CaClassLikeSymbol
        get() = withValidityAssertion { analysisSession.requireClassLikePublicSymbol(coneType) }

    override val qualifiers: List<CaResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            listOf(
                CaCfirResolvedClassTypeQualifierImpl(
                    name = classId.shortClassName,
                    typeArguments = coneType.asPublicTypeProjections(analysisSession),
                    symbol = symbol,
                    token = token,
                )
            )
        }

    override val typeArguments: List<CaType>
        get() = withValidityAssertion {
            qualifiers.last().typeArguments.mapNotNull { projection -> projection.type }
        }

    override fun createPointer(): CaTypePointer<CaUsualClassType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreUsualClassType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
