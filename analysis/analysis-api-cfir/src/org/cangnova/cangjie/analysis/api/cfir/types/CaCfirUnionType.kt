package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreUnionType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 仓颉 union public type 叶子。
 */
internal class CaCfirUnionType(
    override val coneType: ConeUnionType,
    private val analysisSession: CaCfirSession,
) : CaUnionType, CaCfirType {
    override val token: CaLifetimeToken
        get() = analysisSession.token

    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { analysisSession.cfirSymbolBuilder.buildAbbreviatedType(coneType) }

    override val alternatives: List<CaType>
        get() = withValidityAssertion {
            coneType.unionTypes.map { alternative -> alternative.asCaType(analysisSession) }
        }

    override fun createPointer(): CaTypePointer<CaUnionType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreUnionType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
