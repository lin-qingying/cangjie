package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.diagnosticReporter
import org.cangnova.cangjie.name.Name

internal class CfirStatusResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
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

class CfirStatusComputationSession(
    val useSiteSession: CfirSession,
    val useSiteScopeSession: ScopeSession,
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

    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirDeclaration) {
            @Suppress("UNCHECKED_CAST")
            return transformDeclaration(element, data) as E
        }
        return super.transformElement(element, data)
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        declaration.transformChildren(this, data)
        processDeclaration(declaration)
        return declaration
    }

    protected open fun processDeclaration(target: CfirDeclaration) {
        if (target.resolvePhase < CfirResolvePhase.TYPES || target.resolvePhase >= CfirResolvePhase.STATUS) return
        val computationStatus = statusComputationSession.startComputing(target)
        if (computationStatus != CfirStatusComputationSession.StatusComputationStatus.Computed) {
            transformDeclaration(target)
        }
        target.replaceResolvePhase(CfirResolvePhase.STATUS)
        statusComputationSession.endComputing(target)
    }

    protected open fun transformDeclaration(target: CfirDeclaration) {}

    protected fun reportStaticCannotBeOpenAbstractOverride(target: CfirDeclaration) {
        diagnosticReporter.reportOn(
            source = target.source,
            factory = CfirErrors.STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE,
            a = target.declarationNameOrNull,
            context = DiagnosticContext.Default,
        )
    }

    protected fun reportMutOnlyOnFunction(target: CfirDeclaration) {
        diagnosticReporter.reportOn(
            source = target.source,
            factory = CfirErrors.MUT_ONLY_ON_FUNCTION,
            a = target.declarationNameOrNull,
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
            reportStaticCannotBeOpenAbstractOverride(target)
        }
        if (status.isMut && target !is CfirFunction) {
            reportMutOnlyOnFunction(target)
        }
    }
}

/**
 * 从具体声明类型中提取 [CfirDeclarationStatus]。
 * `CfirMemberDeclaration` 本身不直接持有 `status`，因此这里按具体子类分发。
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

private val CfirDeclaration.declarationNameOrNull: Name?
    get() = when (this) {
        is CfirClass -> name
        is CfirFunction -> name
        is CfirProperty -> name
        is CfirFieldVariable -> name
        is CfirTypeAlias -> name
        else -> null
    }

