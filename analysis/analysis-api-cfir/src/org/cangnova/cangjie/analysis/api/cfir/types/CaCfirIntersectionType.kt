package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreIntersectionType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 仓颉 intersection public type 叶子。
 */
internal class CaCfirIntersectionType(
    override val coneType: ConeIntersectionType,
    private val analysisSession: CaCfirSession,
) : CaIntersectionType, CaCfirType {
    override val token: CaLifetimeToken
        get() = analysisSession.token

    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { analysisSession.cfirSymbolBuilder.buildAbbreviatedType(coneType) }

    override val conjuncts: List<CaType>
        get() = withValidityAssertion {
            coneType.intersectedTypes.map { conjunct -> conjunct.asCaType(analysisSession) }
        }

    override fun createPointer(): CaTypePointer<CaIntersectionType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreIntersectionType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
