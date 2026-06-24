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

/**
 * tower resolve 候选收集器。
 *
 * 该收集器按适用性和 tower group 优先级保留当前最佳候选集合，
 * 同时收集需要转发到最终诊断阶段的解析诊断。
 */
open class CfirCandidateCollector(
    /**
     * 当前 body resolve transformer 组件。
     */
    val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    /**
     * 候选阶段检查运行器。
     */
    private val resolutionStageRunner: ResolutionStageRunner,
) {
    /**
     * 当前最佳候选集合。
     */
    private val candidates = mutableListOf<Candidate>()
    /**
     * 需要转发的解析诊断。
     */
    private val forwardedDiagnostics = mutableListOf<ResolutionDiagnostic>()
    /**
     * 函数名作为值时收集的函数候选。
     */
    private val functionValueCandidates = mutableListOf<Candidate>()
    /**
     * 函数值候选所属的最佳 tower group。
     */
    private var functionValueCandidatesGroup: CfirTowerGroup? = null

    /**
     * 当前最佳适用性级别。
     */
    var currentApplicability: CandidateApplicability = CandidateApplicability.HIDDEN
        private set

    /**
     * 当前最佳候选所属 tower group。
     */
    var bestGroup: CfirTowerGroup? = null
        private set

    /**
     * 开始一轮新的候选收集。
     */
    open fun newDataSet() {
        candidates.clear()
        forwardedDiagnostics.clear()
        functionValueCandidates.clear()
        functionValueCandidatesGroup = null
        currentApplicability = CandidateApplicability.HIDDEN
        bestGroup = null
    }

    /**
     * 处理一个候选并根据适用性更新最佳候选集合。
     */
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

    /**
     * 追加需要转发的诊断。
     */
    fun addForwardedDiagnostic(diagnostic: ResolutionDiagnostic) {
        forwardedDiagnostics += diagnostic
    }

    /**
     * 返回已转发诊断列表。
     */
    fun forwardedDiagnostics(): List<ResolutionDiagnostic> = forwardedDiagnostics

    /**
     * 返回当前最佳候选集合。
     */
    fun bestCandidates(): List<Candidate> = candidates

    /**
     * 无目标类型的函数名作为值使用时，同一作用域中的函数候选必须先作为重载集合保留。
     *
     * 官方 Cangjie 对 `let f = obj.foo<T>` 这类表达式会先诊断函数引用歧义；
     * 只有单候选时才下沉到该候选自身的泛型约束错误。
     */
    fun functionValueCandidates(): List<Candidate> = functionValueCandidates

    /**
     * 在函数名作为值使用时记录同组函数候选。
     */
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

    /**
     * 判断 tower resolve 是否应停止在指定 group 之前。
     */
    open fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean {
        val currentBestGroup = bestGroup ?: return false
        return shouldStopResolve && currentBestGroup < group
    }

    /**
     * 当前适用性是否允许停止后续 tower group。
     */
    val shouldStopResolve: Boolean
        get() = currentApplicability.shouldStopResolve

    /**
     * 当前最佳适用性是否代表成功候选。
     */
    @OptIn(ApplicabilityDetail::class)
    val isSuccess: Boolean
        get() = currentApplicability.isSuccess
}

/**
 * 收集所有候选的候选收集器。
 *
 * 主要用于需要查看全部符号候选的诊断或分析路径。
 */
open class CfirAllCandidatesCollector(
    components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionStageRunner: ResolutionStageRunner,
) : CfirCandidateCollector(components, resolutionStageRunner) {
    /**
     * 按符号去重保存的全部候选。
     */
    private val allCandidates = LinkedHashMap<org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>, Candidate>()

    /**
     * 保存候选后继续走普通最佳候选收集逻辑。
     */
    override fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
        context: ResolutionContext,
    ): CandidateApplicability {
        allCandidates.putIfAbsent(candidate.symbol, candidate)
        return super.consumeCandidate(group, candidate, context)
    }

    /**
     * 全候选收集器不会因已有最佳候选而停止后续 group。
     */
    override fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean = false

    /**
     * 返回全部候选集合。
     */
    fun allCandidates(): Collection<Candidate> = allCandidates.values

    /**
     * 重置最佳候选和全部候选缓存。
     */
    override fun newDataSet() {
        super.newDataSet()
        allCandidates.clear()
    }
}
