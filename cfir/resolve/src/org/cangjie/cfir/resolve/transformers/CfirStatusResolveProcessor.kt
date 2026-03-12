package org.cangjie.cfir.resolve.transformers

import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.declarations.CfirFunction
import org.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangjie.cfir.diagnostics.reportOn
import org.cangjie.cfir.scopes.CfirScopeSession
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.diagnosticReporter
import org.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog

internal class CfirStatusResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.STATUS,
) {
    override val transformer: CfirStatusResolveTransformer = run {
        val statusComputationSession = CfirStatusComputationSession(session, scopeSession)
        CfirStatusResolveTransformer(statusComputationSession)
    }
}

private val RULE_STATUS_MODIFIER_LEGALITY = CfirResolveRuleCatalog.STATUS_MODIFIER_LEGALITY

class CfirStatusComputationSession(
    val useSiteSession: CfirSession,
    val useSiteScopeSession: CfirScopeSession,
) {
    private val statusMap: MutableMap<CfirDeclaration, StatusComputationStatus> = hashMapOf()
        .withDefault { StatusComputationStatus.NotComputed }

    operator fun get(declaration: CfirDeclaration): StatusComputationStatus = statusMap.getValue(declaration)

    fun startComputing(declaration: CfirDeclaration): StatusComputationStatus {
        return statusMap.getOrPut(declaration) { StatusComputationStatus.Computing }
    }

    fun endComputing(declaration: CfirDeclaration) {
        statusMap[declaration] = StatusComputationStatus.Computed
    }

    fun computeOnlyDeclarationStatus(declaration: CfirDeclaration) {
        val existedStatus = statusMap.getValue(declaration)
        if (existedStatus < StatusComputationStatus.ComputedOnlyDeclarationStatus) {
            statusMap[declaration] = StatusComputationStatus.ComputedOnlyDeclarationStatus
        }
    }

    enum class StatusComputationStatus(val requiresComputation: Boolean) {
        NotComputed(true),
        Computing(false),
        ComputedOnlyDeclarationStatus(true),
        Computed(false),
    }

    open fun forceResolveStatusesOfSupertypes(declaration: CfirDeclaration) = Unit
}

open class AbstractCfirStatusResolveTransformer(
    val statusComputationSession: CfirStatusComputationSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.STATUS) {
    override val session: CfirSession
        get() = statusComputationSession.useSiteSession

    private val diagnosticReporter: CfirDiagnosticReporter
        get() = statusComputationSession.useSiteSession.diagnosticReporter

    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        processDeclaration(declaration)
        return declaration
    }

    protected open fun processDeclaration(target: CfirDeclaration) {
        if (target.resolvePhase < CfirResolvePhase.TYPES || target.resolvePhase >= CfirResolvePhase.STATUS) return
        val computationStatus = statusComputationSession.startComputing(target)
        if (computationStatus != CfirStatusComputationSession.StatusComputationStatus.Computed) {
            transformDeclaration(target)
        }
        target.resolvePhase = CfirResolvePhase.STATUS
        statusComputationSession.endComputing(target)
    }

    protected open fun transformDeclaration(target: CfirDeclaration) {}

    protected fun reportStatusModifierLegalityError(
        target: CfirDeclaration,
        message: String,
    ) {
        diagnosticReporter.reportOn(
            source = target.source,
            factory = CfirErrors.STATUS_MODIFIER_LEGALITY,
            a = RULE_STATUS_MODIFIER_LEGALITY.id,
            b = "$message (${RULE_STATUS_MODIFIER_LEGALITY.officialReference})",
            context = DiagnosticContext.Default,
        )
    }
}

open class CfirStatusResolveTransformer(
    statusComputationSession: CfirStatusComputationSession,
) : AbstractCfirStatusResolveTransformer(
    statusComputationSession = statusComputationSession,
) {
    override fun transformDeclaration(target: CfirDeclaration) {
        val memberTarget = target as? CfirMemberDeclaration
        if (memberTarget == null) return
        val status = memberTarget.status

        if (status.isStatic && (status.isOpen || status.isAbstract || status.isOverride)) {
            reportStatusModifierLegalityError(target, "static declaration cannot be open/abstract/override")
        }
        if (status.isInline && status.isForeign) {
            reportStatusModifierLegalityError(target, "inline declaration cannot be foreign")
        }
        if (status.isMut && target !is CfirFunction) {
            reportStatusModifierLegalityError(target, "mut modifier is only valid on function declarations")
        }
    }
}
