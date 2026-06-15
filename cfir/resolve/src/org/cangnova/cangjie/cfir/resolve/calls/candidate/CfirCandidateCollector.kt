package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
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
    private val functionValueCandidates = mutableListOf<Candidate>()
    private var functionValueCandidatesGroup: CfirTowerGroup? = null

    var currentApplicability: CandidateApplicability = CandidateApplicability.HIDDEN
        private set

    var bestGroup: CfirTowerGroup? = null
        private set

    open fun newDataSet() {
        candidates.clear()
        forwardedDiagnostics.clear()
        functionValueCandidates.clear()
        functionValueCandidatesGroup = null
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
        recordFunctionValueCandidate(group, candidate)

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

    /**
     * 无目标类型的函数名作为值使用时，同一作用域中的函数候选必须先作为重载集合保留。
     *
     * 官方 Cangjie 对 `let f = obj.foo<T>` 这类表达式会先诊断函数引用歧义；
     * 只有单候选时才下沉到该候选自身的泛型约束错误。
     */
    fun functionValueCandidates(): List<Candidate> = functionValueCandidates

    private fun recordFunctionValueCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
    ) {
        if (candidate.callInfo.callKind != CallKind.NamedValueAccess) return
        if (candidate.symbol.takeIf { it.isBound }?.cfir !is CfirFunction) return

        val currentGroup = functionValueCandidatesGroup
        if (currentGroup == null || group < currentGroup) {
            functionValueCandidates.clear()
            functionValueCandidatesGroup = group
        }
        if (group == functionValueCandidatesGroup) {
            functionValueCandidates += candidate
        }
    }

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
