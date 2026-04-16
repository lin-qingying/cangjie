package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.createTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.name.Name

/**
 * 仓颉类型参数 public type 叶子。
 */
internal class CaCfirTypeParameterType(
    override val coneType: ConeTypeParameterType,
    override val analysisSession: CaCfirSession,
) : CaTypeParameterType(), CaCfirType {
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { null }

    override val name: Name
        get() = withValidityAssertion { coneType.lookupTag.name }

    override val symbol
        get() = withValidityAssertion {
            analysisSession.createTypeParameterSymbol(coneType.lookupTag.typeParameterSymbol)
        }

    override fun createPointer(): CaTypePointer<CaTypeParameterType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreTypeParameterType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
