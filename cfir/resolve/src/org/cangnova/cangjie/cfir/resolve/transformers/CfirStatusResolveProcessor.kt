package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.diagnosticReporter
import org.cangnova.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog

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
    private val statusMap: MutableMap<CfirDeclaration, StatusComputationStatus> =
        hashMapOf<CfirDeclaration, StatusComputationStatus>()
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
        val status = target.statusOrNull ?: return

        if (status.isStatic && (status.isOpen || status.isAbstract || status.isOverride)) {
            reportStatusModifierLegalityError(target, "static declaration cannot be open/abstract/override")
        }
        if (status.isMut && target !is CfirFunction) {
            reportStatusModifierLegalityError(target, "mut modifier is only valid on function declarations")
        }
    }
}

/**
 * 从具体声明类型中提取 [CfirDeclarationStatus]。
 *
 * `CfirMemberDeclaration` 不直接持有 `status`，
 * 该属性分散定义在各具体子类中（CfirClass、CfirFunction 等）。
 */
private val CfirDeclaration.statusOrNull: CfirDeclarationStatus?
    get() = when (this) {
        is CfirClass -> status
        is CfirFunction -> status
        is CfirProperty -> status
        is CfirVariable -> status
        is CfirExtend -> status
        is CfirTypeAlias -> status
        else -> null
    }
