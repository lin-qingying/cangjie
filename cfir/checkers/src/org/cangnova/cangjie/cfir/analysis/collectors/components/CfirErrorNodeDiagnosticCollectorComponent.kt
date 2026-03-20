package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.toCfirDiagnostics
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.references.CfirErrorReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.psi.CjNodeTypes

/**
 * Kotlin-style error-node collector for CFIR diagnostics.
 *
 * Responsibilities:
 * - collect diagnostics from error nodes (`CfirErrorTypeRef`/`CfirErrorExpression`/`CfirErrorReference`)
 * - collect diagnostics carried by `ConeErrorType`
 * - suppress diagnostics on synthetic/fake source kinds where dedicated checkers report instead
 * - avoid duplicate reports from overlapping nodes
 */
class CfirErrorNodeDiagnosticCollectorComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    private val reportedConeDiagnostics = mutableSetOf<ReportedConeDiagnosticKey>()

    override fun visitFunctionCall(functionCall: CfirFunctionCall, data: CheckerContext) {
        processConeTypeDiagnostic(functionCall, functionCall.coneTypeOrNull, functionCall.source, data)
        processErrorReference(functionCall.calleeReference, data)
    }

    override fun visitPropertyAccess(propertyAccess: CfirPropertyAccess, data: CheckerContext) {
        processConeTypeDiagnostic(propertyAccess, propertyAccess.coneTypeOrNull, propertyAccess.source, data)
        processErrorReference(propertyAccess.calleeReference, data)
    }

    override fun visitQualifiedAccess(qualifiedAccess: CfirQualifiedAccess, data: CheckerContext) {
        processConeTypeDiagnostic(qualifiedAccess, qualifiedAccess.coneTypeOrNull, qualifiedAccess.source, data)
        processErrorReference(qualifiedAccess.calleeReference, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: CheckerContext) {
        processConeTypeDiagnostic(resolvedTypeRef, resolvedTypeRef.coneType, resolvedTypeRef.source, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: CheckerContext) {
        return
    }

    override fun visitErrorExpression(errorExpression: CfirErrorExpression, data: CheckerContext) {
        val source = errorExpression.source as? CjSourceElement ?: return
        processConeTypeDiagnostic(errorExpression, errorExpression.coneTypeOrNull, source, data)

        return
    }

    override fun visitErrorReference(errorReference: CfirErrorReference, data: CheckerContext) {
        return
    }

    private fun processErrorReference(reference: CfirReference, context: CheckerContext) {
        val errorReference = reference as? CfirErrorReference ?: return
        val callOrAssignment = context.callsOrAssignments.lastOrNull { it.toReferenceOrNull() === reference }
        if (callOrAssignment is CfirExpression && callOrAssignment.hasUnresolvedReceiver()) {
            return
        }
        visitErrorReference(errorReference, context)
    }

    private fun processConeTypeDiagnostic(
        owner: CfirElement,
        coneType: ConeCangjieType?,
        source: AbstractCjSourceElement?,
        context: CheckerContext,
    ) {
        val sourceElement = source as? CjSourceElement ?: return
        val diagnostic = (coneType as? ConeErrorType)?.diagnostic ?: return

        val callOrAssignment = findOwningCallOrAssignment(owner, context)
        val callOrAssignmentSource = callOrAssignment?.source as? CjSourceElement

        reportConeDiagnostic(diagnostic, sourceElement, context, callOrAssignmentSource)
    }

    private fun findOwningCallOrAssignment(owner: CfirElement, context: CheckerContext): CfirElement? {
        return when (owner) {
            is CfirFunctionCall,
            is CfirPropertyAccess,
            is CfirQualifiedAccess,
            is CfirAssignment -> owner

            else -> context.callsOrAssignments.lastOrNull()
        }
    }

    private fun reportConeDiagnostic(
        diagnostic: ConeDiagnostic,
        source: CjSourceElement?,
        context: CheckerContext,
        callOrAssignmentSource: CjSourceElement? = null,
    ) {
        if (source == null) return

        if (source.elementType == CjNodeTypes.ANNOTATION && diagnostic is ConeUnresolvedNameError) return
        if (source.kind == CjFakeSourceElementKind.ArrayAccessNameReference && diagnostic is ConeUnresolvedNameError) return

        val key = ReportedConeDiagnosticKey(
            reason = diagnostic.reason,
            sourceStart = source.startOffset,
            sourceEnd = source.endOffset,
            callStart = callOrAssignmentSource?.startOffset,
            callEnd = callOrAssignmentSource?.endOffset,
        )
        if (!reportedConeDiagnostics.add(key)) return

        reportCfirDiagnostic(diagnostic, source, context, callOrAssignmentSource)
    }

    private fun CfirElement.toReferenceOrNull(): CfirReference? {
        return when (this) {
            is CfirFunctionCall -> calleeReference
            is CfirPropertyAccess -> calleeReference
            is CfirQualifiedAccess -> calleeReference
            is CfirAssignment -> lValue.toReferenceOrNull()
            else -> null
        }
    }

    private fun CfirExpression.hasUnresolvedReceiver(): Boolean {
        val receiver = when (this) {
            is CfirFunctionCall -> explicitReceiver
            is CfirPropertyAccess -> explicitReceiver
            is CfirQualifiedAccess -> explicitReceiver
            else -> null
        }
        return receiver.cannotBeResolved()
    }

    private fun CfirExpression?.cannotBeResolved(): Boolean {
        val diagnostic = ((this?.coneTypeOrNull as? ConeErrorType)?.diagnostic) ?: return false
        return diagnostic is ConeUnresolvedNameError ||
                diagnostic is ConeUnresolvedReferenceError ||
                diagnostic is ConeUnresolvedSymbolError
    }

    private data class ReportedConeDiagnosticKey(
        val reason: String,
        val sourceStart: Int,
        val sourceEnd: Int,
        val callStart: Int?,
        val callEnd: Int?,
    )



    private fun reportCfirDiagnostic(
        diagnostic: ConeDiagnostic,
        source: CjSourceElement?,
        context: CheckerContext,
        callOrAssignmentSource: CjSourceElement? = null,
    ) {
        reportCfirDiagnostic(
            diagnostic,
            source,
            context,
            session,
            reporter,
            callOrAssignmentSource,
            valueParameter = null
        )
    }

    companion object {
        internal fun reportCfirDiagnostic(
            diagnostic: ConeDiagnostic,
            source: CjSourceElement?,
            context: CheckerContext,
            session: CfirSession = context.session,
            reporter: DiagnosticReporter,
            callOrAssignmentSource: CjSourceElement? = null,
            valueParameter: CfirValueParameter? = null,
        ) {


            // Will be handled by [FirDelegatedPropertyChecker]
            if (source?.kind == CjFakeSourceElementKind.DelegatedPropertyAccessor &&
                (diagnostic is ConeUnresolvedNameError || diagnostic is ConeAmbiguityError ||   diagnostic is ConeInapplicableCandidateError)
            ) {
                return
            }

            if (source?.kind == CjFakeSourceElementKind.ImplicitConstructor || source?.kind == CjFakeSourceElementKind.DesugaredForLoop) {
                // See FirForLoopChecker
                return
            }

            // Prefix inc/dec on array access will have two calls to .get(...), don't report for the second one.
            if (source?.kind is CjFakeSourceElementKind.DesugaredPrefixSecondGetReference) {
                return
            }

            // If something is wrong with the `when` subject access, then there's already an error on the `when` subject itself.
            if (source?.kind is CjFakeSourceElementKind.UnresolvedWhenConditionSubject) {
                return
            }

            for (coneDiagnostic in diagnostic.toCfirDiagnostics(session, source, callOrAssignmentSource, valueParameter)) {
                reporter.report(coneDiagnostic, context)
            }
        }
    }

}
