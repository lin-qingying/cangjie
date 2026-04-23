package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restorePrimitiveType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 基本类型 public 叶子，对齐 CFIR `ConePrimitiveType(kind)` 模型。
 */
internal class CaCfirPrimitiveType(
    override val coneType: ConePrimitiveType,
    private val analysisSession: CaCfirSession,
) : CaPrimitiveType(), CaCfirType {
    override val token: CaLifetimeToken
        get() = analysisSession.token

    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { null }

    override val kind: PrimitiveTypeKind
        get() = withValidityAssertion { coneType.kind }

    override fun createPointer(): CaTypePointer<CaPrimitiveType> = withValidityAssertion {
        createTypePointer(coneType, ::restorePrimitiveType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
