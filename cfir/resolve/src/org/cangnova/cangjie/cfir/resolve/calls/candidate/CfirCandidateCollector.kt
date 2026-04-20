package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability.RESOLVED_LOW_PRIORITY
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.resolve.calls.tower.shouldStopResolve

open class CfirCandidateCollector(
    val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: ResolutionStageRunner,
) {
    private val candidates = mutableListOf<Candidate>()
    private val forwardedDiagnostics = mutableListOf<ResolutionDiagnostic>()

    var currentApplicability: CandidateApplicability = CandidateApplicability.HIDDEN
        private set

    var bestGroup: CfirTowerGroup? = null
        private set

    open fun newDataSet() {
        candidates.clear()
        forwardedDiagnostics.clear()
        currentApplicability = CandidateApplicability.HIDDEN
        bestGroup = null
    }

    open fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
        context: ResolutionContext,
    ): CandidateApplicability {
        val applicability = resolutionStageRunner.processCandidate(candidate, context)
        val currentBestGroup = bestGroup

        if (
            applicability > currentApplicability ||
            (applicability == currentApplicability && (currentBestGroup == null || group < currentBestGroup))
        ) {
            if (applicability >= RESOLVED_LOW_PRIORITY) {
                candidates.clear()
            }

            currentApplicability = applicability
            bestGroup = group
        }

        if (
            (applicability == currentApplicability && group == bestGroup) ||
            (currentApplicability == INAPPLICABLE_ARGUMENTS_MAPPING_ERROR && applicability == INAPPLICABLE_WRONG_RECEIVER)
        ) {
            candidates += candidate
        }

        return applicability
    }

    fun addForwardedDiagnostic(diagnostic: ResolutionDiagnostic) {
        forwardedDiagnostics += diagnostic
    }

    fun forwardedDiagnostics(): List<ResolutionDiagnostic> = forwardedDiagnostics

    fun bestCandidates(): List<Candidate> = candidates

    open fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean {
        val currentBestGroup = bestGroup ?: return false
        return shouldStopResolve && currentBestGroup < group
    }

    val shouldStopResolve: Boolean
        get() = currentApplicability.shouldStopResolve

    @OptIn(ApplicabilityDetail::class)
    val isSuccess: Boolean
        get() = currentApplicability.isSuccess
}

open class CfirAllCandidatesCollector(
    components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionStageRunner: ResolutionStageRunner,
) : CfirCandidateCollector(components, resolutionStageRunner) {
    private val allCandidates = LinkedHashMap<org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>, Candidate>()

    override fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
        context: ResolutionContext,
    ): CandidateApplicability {
        allCandidates.putIfAbsent(candidate.symbol, candidate)
        return super.consumeCandidate(group, candidate, context)
    }

    override fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean = false

    fun allCandidates(): Collection<Candidate> = allCandidates.values

    override fun newDataSet() {
        super.newDataSet()
        allCandidates.clear()
    }
}
