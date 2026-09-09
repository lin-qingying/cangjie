package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupDisposition
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability.RESOLVED_LOW_PRIORITY
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.resolve.calls.tower.shouldStopResolve

/**
 * callable 名称发现阶段产生、但不会创建普通 [Candidate] 的结构化结果。
 *
 * [Excluded] 表示声明已在当前 tower group 被发现，但官方调用语义要求把它排除在
 * overload 集合之外。该状态必须进入 collector，才能同时约束 tower 截止与最终诊断，
 * 不能只停留在某个 tower level 的局部返回值中。
 */
sealed interface CfirCallableLookupOutcome {
    /** 发现声明的 tower group。 */
    val group: CfirTowerGroup

    /** 被结构性发现的 callable。 */
    val symbol: CfirCallableSymbol<*>

    /** effective member graph 保留下来的完整查找来源。 */
    val lookupProvenance: CfirCallableLookupProvenance

    /** 统一 accessibility checker 给出的使用点结果。 */
    val accessibilityResult: CfirAccessibilityResult.Inaccessible

    /**
     * 名字已发现但必须排除出调用候选集合。
     *
     * 普通访问控制候选不会使用该类型；`REPORT_ACCESS_ERROR` 仍创建 [Candidate]，
     * `NOT_DISCOVERABLE` 则完全不留下名称发现结果。
     */
    data class Excluded(
        override val group: CfirTowerGroup,
        override val symbol: CfirCallableSymbol<*>,
        override val lookupProvenance: CfirCallableLookupProvenance,
        override val accessibilityResult: CfirAccessibilityResult.Inaccessible,
        /** 显式接收者或非函数声明会截止外层查找；无限定函数只按签名遮蔽。 */
        val restrictsOuterScopes: Boolean,
    ) : CfirCallableLookupOutcome {
        init {
            require(accessibilityResult.disposition == CfirLookupDisposition.EXCLUDE_CALLABLE) {
                "Excluded callable lookup outcome requires EXCLUDE_CALLABLE disposition"
            }
        }
    }
}

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
    /** tower 每个 group 实际发现的全部候选，不按适用性等级删除。 */
    private val discoveredCandidatesByGroup = linkedMapOf<CfirTowerGroup, MutableList<Candidate>>()
    /** tower 每个 group 中被调用语义排除的结构性 callable。 */
    private val excludedCallableLookupsByGroup =
        linkedMapOf<CfirTowerGroup, LinkedHashMap<CfirCandidateLookupIdentity, CfirCallableLookupOutcome.Excluded>>()
    /** 诊断使用的最高优先级排除组，不等同于词法查找截止面。 */
    private var bestExcludedCallableGroup: CfirTowerGroup? = null
    /** 形成名称查找截止面的最高优先级 excluded-callable group。 */
    private var excludedCallableBarrierGroup: CfirTowerGroup? = null
    /**
     * 需要转发的解析诊断。
     */
    private val forwardedDiagnostics = linkedSetOf<ResolutionDiagnostic>()
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
        discoveredCandidatesByGroup.clear()
        excludedCallableLookupsByGroup.clear()
        bestExcludedCallableGroup = null
        excludedCallableBarrierGroup = null
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
        discoveredCandidatesByGroup.getOrPut(group) { mutableListOf() } += candidate
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
     * 只按更近作用域已发现的声明签名排除外层函数，不使用实参或返回类型适用性。
     * 在创建 Candidate 前调用，所有候选收集入口都不会得到被词法遮蔽的伪候选。
     */
    internal fun isShadowedInCloserScope(group: CfirTowerGroup, symbol: CfirCallableSymbol<*>): Boolean {
        val function = symbol.cfir as? CfirNamedFunction ?: return false
        fun shadows(symbol: CfirCallableSymbol<*>): Boolean {
            val closer = symbol.cfir as? CfirNamedFunction ?: return false
            return closer.hasSameParameterSignatureForScopeShadowing(function, components.session)
        }
        return discoveredCandidatesByGroup.any { (discoveredGroup, discoveries) ->
            discoveredGroup.isCloserLexicalScopeThan(group) && discoveries.any { discovery ->
                (discovery.symbol as? CfirCallableSymbol<*>)?.let(::shadows) == true
            }
        } || excludedCallableLookupsByGroup.any { (discoveredGroup, discoveries) ->
            discoveredGroup.isCloserLexicalScopeThan(group) && discoveries.values.any { discovery ->
                shadows(discovery.symbol)
            }
        }
    }

    /**
     * 记录名字已发现但被排除出 overload 集合的 callable。
     *
     * 该结果不创建伪候选，也不改变候选适用性。普通函数仍参与同签名遮蔽，但不阻断
     * 外层不同签名重载；最终没有可用候选时由调用 resolver 产生 no-match。
     */
    fun consumeLookupOutcome(outcome: CfirCallableLookupOutcome.Excluded) {
        val bestExcluded = bestExcludedCallableGroup
        if (bestExcluded == null || outcome.group < bestExcluded) {
            bestExcludedCallableGroup = outcome.group
        }
        val barrierGroup = excludedCallableBarrierGroup
        if (outcome.restrictsOuterScopes && (barrierGroup == null || outcome.group < barrierGroup)) {
            excludedCallableBarrierGroup = outcome.group
        }
        excludedCallableLookupsByGroup
            .getOrPut(outcome.group) { linkedMapOf() }
            .putIfAbsent(
                CfirCandidateLookupIdentity(outcome.symbol, outcome.lookupProvenance),
                outcome,
            )
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
    fun forwardedDiagnostics(): List<ResolutionDiagnostic> = forwardedDiagnostics.toList()

    /**
     * 返回当前最佳候选集合。
     */
    fun bestCandidates(): List<Candidate> = candidates

    /**
     * 返回最终最佳 tower group 中实际发现的全部候选。
     *
     * numeric priority 等适用性分层可能让 [bestCandidates] 只保留一个候选，但外层 expected
     * type 细化仍需要同一词法 group 的完整声明集合；更低优先级 tower group 不得被重新引入。
     */
    fun candidatesDiscoveredInBestGroup(): List<Candidate> =
        bestGroup?.let(discoveredCandidatesByGroup::get).orEmpty()

    /** 返回最高优先级排除组的结构发现结果，供最终 no-match 诊断使用。 */
    fun excludedCallableLookupOutcomes(): List<CfirCallableLookupOutcome.Excluded> =
        bestExcludedCallableGroup
            ?.let(excludedCallableLookupsByGroup::get)
            ?.values
            ?.toList()
            .orEmpty()

    /**
     * 无目标类型的函数名作为值使用时，同一作用域中的函数候选必须先作为重载集合保留。
     *
     * 官方 Cangjie 对 `let f = obj.foo<T>` 这类表达式会先诊断函数引用歧义；
     * 只有单候选时才下沉到该候选自身的泛型约束错误。
     */
    fun functionValueCandidates(): List<Candidate> = functionValueCandidates

    /**
     * 返回函数值候选所属的最佳 tower group，供 named-value 跨 callable kind 合并时校验作用域层级。
     */
    fun functionValueCandidatesGroup(): CfirTowerGroup? = functionValueCandidatesGroup

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
        val shouldStopForCandidate = bestGroup?.let { currentBestGroup ->
            shouldStopResolve && currentBestGroup < group
        } == true
        val shouldStopForExcludedCallable = excludedCallableBarrierGroup?.let { barrierGroup ->
            barrierGroup < group
        } == true
        return shouldStopForCandidate || shouldStopForExcludedCallable
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
 * 词法声明层级与 tower 的搜索优先级分开：不同导入 scope 仍处于同一声明层级，
 * extend 与普通成员也不能互相当成外层函数。只有局部深度或声明层级更近才形成遮蔽。
 */
private fun CfirTowerGroup.isCloserLexicalScopeThan(other: CfirTowerGroup): Boolean {
    if (kind == CfirTowerGroup.Kind.EXPLICIT_MEMBER || other.kind == CfirTowerGroup.Kind.EXPLICIT_MEMBER) return false
    if (kind == CfirTowerGroup.Kind.LOCAL && other.kind == CfirTowerGroup.Kind.LOCAL) return depth < other.depth
    fun CfirTowerGroup.Kind.declarationLevel(): Int = when (this) {
        CfirTowerGroup.Kind.EXPLICIT_MEMBER -> 3
        CfirTowerGroup.Kind.LOCAL -> 2
        CfirTowerGroup.Kind.EXTEND, CfirTowerGroup.Kind.IMPLICIT_MEMBER -> 1
        CfirTowerGroup.Kind.NON_LOCAL, CfirTowerGroup.Kind.PACKAGE -> 0
        CfirTowerGroup.Kind.IMPORTED -> -1
    }
    return kind.declarationLevel() > other.kind.declarationLevel()
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
    /** 按 symbol 与完整 lookup provenance 共同去重保存全部结构候选。 */
    private val allCandidates = LinkedHashMap<CfirCandidateLookupIdentity, Candidate>()

    /**
     * 保存候选后继续走普通最佳候选收集逻辑。
     */
    override fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
        context: ResolutionContext,
    ): CandidateApplicability {
        allCandidates.putIfAbsent(candidate.lookupIdentity(), candidate)
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
