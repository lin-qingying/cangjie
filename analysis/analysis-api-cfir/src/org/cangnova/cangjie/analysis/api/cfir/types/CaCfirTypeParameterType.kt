package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreTypeParameterType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
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
    private val builder: CaSymbolByCfirBuilder,

    ) : CaTypeParameterType(), CaCfirType {
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { null }

    override val name: Name
        get() = withValidityAssertion { coneType.lookupTag.name }

    override val symbol: CaTypeParameterSymbol
        get() = withValidityAssertion {
            builder.classifierBuilder.buildTypeParameterSymbol(coneType.lookupTag.typeParameterSymbol)
        }

    override fun createPointer(): CaTypePointer<CaTypeParameterType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreTypeParameterType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
    override val token: CaLifetimeToken get() = builder.token

}
