package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.createClassLikeSymbol
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassErrorType
import org.cangnova.cangjie.analysis.api.types.CaClassTypeQualifier
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 仓颉 class-like error public type 叶子。
 */
internal class CaCfirClassErrorType(
    override val coneType: ConeErrorType,
    override val analysisSession: CaCfirSession,
) : CaClassErrorType(), CaCfirType {
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { null }

    override val errorMessage: String
        get() = withValidityAssertion { coneType.diagnostic.reason }

    override val presentableText: String?
        get() = withValidityAssertion { coneType.delegatedType?.renderForDebugging() }

    override val qualifiers: List<CaClassTypeQualifier>
        get() = withValidityAssertion {
            val candidateSymbol = candidateSymbols.singleOrNull() ?: return@withValidityAssertion emptyList()
            listOf(
                CaCfirResolvedClassTypeQualifierImpl(
                    name = candidateSymbol.classId?.shortClassName ?: coneType.lookupTag.classId.shortClassName,
                    typeArguments = emptyList(),
                    symbol = candidateSymbol,
                    token = token,
                )
            )
        }

    override val candidateSymbols: Collection<CaClassLikeSymbol>
        get() = withValidityAssertion {
            val diagnostic = coneType.diagnostic
            when (diagnostic) {
                is ConeDiagnosticWithCandidates -> diagnostic.candidateSymbols
                    .filterIsInstance<CfirClassLikeSymbol<*>>()
                    .map { symbol -> analysisSession.createClassLikeSymbol(symbol) }

                else -> analysisSession.queryTypeClassLikeSymbol(coneType.delegatedType ?: coneType)
                    ?.let { symbol -> analysisSession.createClassLikeSymbol(symbol) }
                    ?.let(::listOf)
                    .orEmpty()
            }
        }

    override fun createPointer(): CaTypePointer<CaClassErrorType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreClassErrorType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
