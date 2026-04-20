package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreTupleType
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 仓颉 tuple public type 叶子。
 */
internal class CaCfirTupleType(
    override val coneType: ConeTupleType,
    override val analysisSession: CaCfirSession,
) : CaTupleType, CaCfirType {
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { null }

    override val elementTypes: List<CaType>
        get() = withValidityAssertion {
            coneType.elementTypes.map { elementType -> elementType.asCaType(analysisSession) }
        }

    override fun createPointer(): CaTypePointer<CaTupleType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreTupleType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
