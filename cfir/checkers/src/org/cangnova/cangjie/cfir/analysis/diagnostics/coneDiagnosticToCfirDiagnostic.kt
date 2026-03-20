package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.diagnostic.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory2
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement

fun ConeDiagnostic.toCfirDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    valueParameter: CfirValueParameter? = null,
): List<CjDiagnostic> {
    return when (this) {
        is ConeInapplicableCandidateError -> mapInapplicableCandidateError(session, source, callOrAssignmentSource)
        is ConeAmbiguityError -> mapConeAmbiguityError(source, callOrAssignmentSource, session)
        else -> listOfNotNull(mapOtherDiagnostic(source, valueParameter, callOrAssignmentSource, session))
    }
}

private fun ConeInapplicableCandidateError.mapInapplicableCandidateError(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    val diagnosticSource = qualifiedAccessSource ?: source ?: return emptyList()
    val candidateName = candidateSymbol.debugName
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateName, null, session))
}

private fun ConeAmbiguityError.mapConeAmbiguityError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
    val referenceName = name.asString()
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, referenceName, null, session))
}

private fun ConeDiagnostic.mapOtherDiagnostic(
    source: CjSourceElement?,
    valueParameter: CfirValueParameter?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return null
    return when (this) {
        is ConeCannotInferTypeParameterType ->
            CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, typeParameter, session)

        is ConeSimpleDiagnostic -> when (kind) {
            DiagnosticKind.CannotInferParameterType -> {
                // If a dedicated parameter context is available, mirror FIR and report CANNOT_INFER_PARAMETER_TYPE.
                valueParameter
                    ?.typeParameters
                    ?.firstOrNull()
                    ?.symbol
                    ?.let { it as? CfirTypeParameterSymbol }
                    ?.let { CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, it, session) }
            }

            else -> null
        }
        is ConeUnresolvedNameError -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, name.asString(), operator, session)
        is ConeUnresolvedReferenceError -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, name.asString(), null, session)
        is ConeUnresolvedSymbolError -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, classId.asString(), null, session)
        else -> null
    }
}

private fun diagnosticContext(session: CfirSession): DiagnosticContext {
    return object : DiagnosticContext {
        override val languageVersionSettings = session.languageVersionSettings
        override val containingFilePath: String? = null
        override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
    }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A> CjDiagnosticFactory1<A>.on(
    source: AbstractCjSourceElement,
    a: A,
    session: CfirSession,
): CjDiagnostic? = on(source, a, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory2<String, String?>.on(
    source: AbstractCjSourceElement,
    a: String,
    b: String?,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, null, diagnosticContext(session))
