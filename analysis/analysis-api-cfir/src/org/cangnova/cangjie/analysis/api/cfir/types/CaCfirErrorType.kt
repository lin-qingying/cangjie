package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.createPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.api.util.requireIsInstance
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.renderForDebugging

internal class CaCfirErrorType(
    override val coneType: ConeErrorType,
    private val builder: CaSymbolByCfirBuilder,
) : CaErrorType, CaCfirType {
    override val token: CaLifetimeToken get() = builder.token
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val errorMessage: String
        get() = withValidityAssertion { coneType.diagnostic.reason }


    override val presentableText: String?
        get() = withValidityAssertion {
            when (val diagnostic = coneType.diagnostic) {
                is ConeCannotInferTypeParameterType -> diagnostic.typeParameter.name.asString()
                else -> coneType.delegatedType?.renderForDebugging()
            }
        }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
        emptyTypeAnnotations(token)
        }

    override val abbreviation: CaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @CaExperimentalApi
    override fun createPointer(): CaTypePointer<CaErrorType> = withValidityAssertion {
        return CaCfirErrorTypePointer(coneType, builder)
    }
}
private class CaCfirErrorTypePointer(
    coneType: ConeErrorType,
    builder: CaSymbolByCfirBuilder,
) : CaTypePointer<CaErrorType> {
    private val coneTypePointer = coneType.createPointer(builder)

    @CaImplementationDetail
    override fun restore(session: CaSession): CaErrorType? = session.withValidityAssertion {
        requireIsInstance<CaCfirSession>(session)

        val coneType = coneTypePointer.restore(session) ?: return null
        return CaCfirErrorType(coneType, session.cfirSymbolBuilder)
    }
}
