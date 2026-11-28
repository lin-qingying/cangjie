package org.cangnova.cangjie.cfir.resolve.inference

import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResourceVariable
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.ImplicitInvokeMode
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.asCone
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidatePCLACompletionState
import org.cangnova.cangjie.cfir.resolve.body.CfirResourceSourceCompletion
import org.cangnova.cangjie.cfir.resolve.body.CfirResourceSourceSnapshot
import org.cangnova.cangjie.cfir.resolve.body.captureResourceSourceStates
import org.cangnova.cangjie.cfir.resolve.body.restoreResourceSourceStates
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 源码资源 initializer 的独立推断图。
 *
 * RAW copier 提供唯一元素和符号映射；所有类型变量槽都以实际节点和语义角色定位。
 * 试算拥有自己的 AST、Candidate、atom 与写回上下文。正式解析重建这些可变对象，
 * 只复用同一次实例化的类型变量，不以名字、ClassId 或源码范围推测身份。
 */
internal class CfirImplicitTypeComputation(
    val resource: CfirResourceVariable,
    var initializer: CfirExpression,
    elementMapping: Map<CfirElement, CfirElement>,
    symbolMapping: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>>,
    private val enclosingScope: CfirImplicitTypeComputationScope?,
    val registry: IdentityHashMap<CfirResourceVariable, CfirImplicitTypeComputation>,
    private val incomingVariables: Set<TypeConstructorMarker>,
) {
    private val cloneToSource = IdentityHashMap<CfirElement, CfirElement>().apply {
        for ((source, clone) in elementMapping) put(clone, enclosingScope?.canonicalElement(source) ?: source)
    }
    private val cloneToSymbol = IdentityHashMap<CfirBasedSymbol<*>, CfirBasedSymbol<*>>().apply {
        for ((source, clone) in symbolMapping) put(clone, enclosingScope?.canonicalSymbol(source) ?: source)
    }
    private val canonicalToPrivate = IdentityHashMap<CfirElement, CfirElement>().apply {
        cloneToSource.forEach { (clone, source) -> check(put(source, clone) == null) }
    }
    private val canonicalToPrivateSymbol = IdentityHashMap<CfirBasedSymbol<*>, CfirBasedSymbol<*>>().apply {
        cloneToSymbol.forEach { (clone, source) -> check(put(source, clone) == null) }
    }

    /** owning closure 之外的声明只沿明确的 enclosing scope 借用，不重新查询 provider。 */
    fun privateElementForCanonical(source: CfirElement): CfirElement =
        canonicalToPrivate[source] ?: enclosingScope?.projectElementFromCanonical(source) ?: source

    fun privateSymbolForCanonical(source: CfirBasedSymbol<*>): CfirBasedSymbol<*> =
        canonicalToPrivateSymbol[source] ?: enclosingScope?.projectSymbolFromCanonical(source) ?: source

    /** 映射只用于元数据身份；原声明的 symbol 始终绑定在原声明。 */
    fun canonicalSymbol(symbol: CfirBasedSymbol<*>): CfirBasedSymbol<*> =
        cloneToSymbol[symbol] ?: enclosingScope?.canonicalSymbol(symbol) ?: symbol

    fun canonicalOwnedPrivateElementOrNull(element: CfirElement): CfirElement? = derivedSites[element] ?: cloneToSource[element]

    fun canonicalPrivateElement(element: CfirElement): CfirElement =
        checkNotNull(cloneToSource[element]) { "A retained call site has no RAW ownership mapping: ${element.javaClass.name}" }

    private val derivedSites = IdentityHashMap<CfirElement, CfirElement>()
    private val syntheticTargets = IdentityHashMap<CfirBasedSymbol<*>, CallTarget.Synthetic>()

    fun derivedSite(element: CfirElement): CfirElement? = derivedSites[element]
    fun ownsPrivateElement(element: CfirElement): Boolean = element in cloneToSource || element in derivedSites

    /** 合成调用必须由构造它的路径显式登记原节点，不接受 offset 或文本匹配。 */
    fun registerDerivedElement(scope: CfirImplicitTypeComputationScope, source: CfirElement, derived: CfirElement) {
        val canonical = scope.canonicalElement(source)
        val previous = derivedSites.put(derived, canonical)
        check(previous == null || previous === canonical)
    }

    fun registerSyntheticSymbol(
        scope: CfirImplicitTypeComputationScope,
        source: CfirElement,
        symbol: CfirBasedSymbol<*>,
        role: CfirSyntheticCallRole,
    ) {
        val target = CallTarget.Synthetic(scope.canonicalElement(source), role)
        val previous = syntheticTargets.put(symbol, target)
        check(previous == null || previous == target)
    }

    private sealed interface CallTarget {
        data class Declaration(val symbol: CfirBasedSymbol<*>) : CallTarget
        data class Synthetic(val source: CfirElement, val role: CfirSyntheticCallRole) : CallTarget
    }

    private fun CfirImplicitTypeComputationScope.target(candidate: Candidate): CallTarget {
        syntheticTargets[candidate.symbol]?.let { registered ->
            check(registered.source === canonicalElement(candidate.callInfo.callSite))
            return registered
        }
        val original = (candidate.symbol as? CfirCallableSymbol<*>)?.unwrapSubstitutionOverrides() ?: candidate.symbol
        return CallTarget.Declaration(canonicalSymbol(original))
    }

    private class CandidateKey(
        val callSite: CfirElement,
        val target: CallTarget,
        val kind: CallKind,
        val origin: CfirFunctionCallOrigin,
        val invokeMode: ImplicitInvokeMode,
        val provenance: CfirCallableLookupProvenance,
    ) {
        override fun equals(other: Any?): Boolean = other is CandidateKey &&
                callSite === other.callSite && target == other.target && kind === other.kind &&
                origin == other.origin && invokeMode === other.invokeMode && provenance == other.provenance

        override fun hashCode(): Int {
            var hash = System.identityHashCode(callSite)
            hash = 31 * hash + target.hashCode()
            hash = 31 * hash + kind.hashCode()
            hash = 31 * hash + origin.hashCode()
            hash = 31 * hash + invokeMode.hashCode()
            return 31 * hash + provenance.hashCode()
        }
    }

    private class VariableSlot(val owner: CfirElement, val role: CfirInferenceVariableRole, val index: Int) {
        override fun equals(other: Any?): Boolean = other is VariableSlot &&
                owner === other.owner && role === other.role && index == other.index
        override fun hashCode(): Int = 31 * (31 * System.identityHashCode(owner) + role.hashCode()) + index
    }

    /** 每次重查拥有独立代次；恢复成功快照可重新激活该代次，撤销轮次不能留下 required 证明。 */
    private class Generation
    private var generation = Generation()

    /** 调用身份故意不含 target/provenance；最终成功后再比较完整 CandidateKey。 */
    private class CallSlot(val site: CfirElement, val kind: CallKind, val origin: CfirFunctionCallOrigin, val invokeMode: ImplicitInvokeMode) {
        override fun equals(other: Any?): Boolean = other is CallSlot && site === other.site &&
                kind === other.kind && origin == other.origin && invokeMode === other.invokeMode
        override fun hashCode(): Int = 31 * (31 * (31 * System.identityHashCode(site) + kind.hashCode()) + origin.hashCode()) + invokeMode.hashCode()
    }
    private fun CandidateKey.callSlot(): CallSlot = CallSlot(callSite, kind, origin, invokeMode)
    /** 每次 PRIVATE 最终发布独立代次；sourceGeneration 只标记对应 AST/CS 快照。 */
    private class Terminal(
        val generation: Generation,
        val sourceGeneration: Generation,
        val instantiation: Instantiation,
        val candidate: Candidate,
        val carrier: CfirExpression,
        val reference: CfirReference,
        val type: ConeCangJieType,
    )
    /** 创建实际 FORMAL writer 时绑定其将写回的实例与 terminal，完成证明不能追认后到代次。 */
    private class FormalCompletionBinding(
        val generation: Generation,
        val instantiation: Instantiation,
        val terminal: Terminal?,
    )
    /** 已执行的 FORMAL 写回保存真实 carrier；后续 factory 只能借此接线，不能继承其完成证明。 */
    private class CompletedFormalCall(
        val binding: FormalCompletionBinding,
        val candidate: Candidate,
        val carrier: CfirExpression,
        val reference: CfirReference,
        val type: ConeCangJieType,
    )
    private data class ExpressionUse(val generation: Generation, val type: ConeCangJieType, val accepted: Boolean)
    private val terminals = mutableMapOf<CallSlot, Terminal>()
    private val formalOutcomes = mutableMapOf<CallSlot, CfirRetainedCallOutcome>()
    private val formalSourceRejections = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    private val privateExpressionUses = mutableMapOf<VariableSlot, ExpressionUse>()
    private val formalExpressionUses = mutableMapOf<VariableSlot, ExpressionUse>()

    private class Instantiation(val key: CandidateKey) {
        var outcome: CfirRetainedCallOutcome = CfirRetainedCallOutcome.SKIPPED
        var terminalTarget: CallTarget? = null
        var storage: ConstraintStorage? = null
        var parameters: List<CfirTypeParameterSymbol>? = null
        var variables: List<ConeTypeParameterBasedTypeVariable>? = null
        val associatedVariables = mutableMapOf<VariableSlot, ConeTypeVariable>()
    }

    private val privateInstantiations = IdentityHashMap<Candidate, Instantiation>()
    private val withdrawnCandidates = Collections.newSetFromMap(IdentityHashMap<Candidate, Boolean>())
    /** 私有图未分析的 RAW 节点与新的候选探索，各自在正式图拥有明确的首次分配事务。 */
    private val formalAllocations = IdentityHashMap<Candidate, Instantiation>()
    /** allocation 可来自尚未归约的 tower 探索；只有实际引用/atom 选择才建立正式写回义务。 */
    private val formalSelectedAllocations = IdentityHashMap<Candidate, Instantiation>()
    private val privateSelectedCandidates = Collections.newSetFromMap(IdentityHashMap<Candidate, Boolean>())
    private val selectedInstantiations = mutableMapOf<CandidateKey, Instantiation>()
    private val expressionVariables = mutableMapOf<VariableSlot, ConeTypeVariable>()
    private val replayedInstantiations = Collections.newSetFromMap(IdentityHashMap<Instantiation, Boolean>())
    private val formalWriterBindings = IdentityHashMap<CfirRetainedCompletionLease, IdentityHashMap<Candidate, FormalCompletionBinding>>()
    private val completedFormalBindings = mutableMapOf<CallSlot, CompletedFormalCall>()

    private fun formalInstantiation(scope: CfirImplicitTypeComputationScope, candidate: Candidate): Instantiation {
        val key = scope.key(candidate)
        formalAllocations[candidate]?.let { return it }
        selectedInstantiations[key]?.takeIf {
            it.outcome == CfirRetainedCallOutcome.PARTIAL || it.outcome == CfirRetainedCallOutcome.COMPLETED
        }?.let { retained ->
            formalAllocations[candidate] = retained
            return retained
        }
        check(canonicalToPrivate.containsKey(key.callSite)) {
            "A first formal allocation has no RAW/derived source provenance: ${key.callSite.javaClass.simpleName}, ${candidate.callInfo.origin}, source=${key.callSite.source?.startOffset}-${key.callSite.source?.endOffset}; " +
                    describeCallSiteProvenance(candidate.callInfo.callSite, key.callSite)
        }
        return formalAllocations.getOrPut(candidate) { Instantiation(key) }
    }

    /** 实际选择同一调用槽的新实例时撤销旧候选，分配行为本身不等于选择。 */
    internal fun selectCompletionCandidate(scope: CfirImplicitTypeComputationScope, candidate: Candidate) {
        check(!retired && registry[resource] === this && candidate !in withdrawnCandidates)
        if (scope.mode == CfirImplicitTypeComputationMode.PRIVATE) {
            val site = scope.key(candidate).callSlot()
            privateInstantiations.entries.filter { (other, instance) -> other !== candidate && instance.key.callSlot() == site }
                .map { it.key }.forEach { previous ->
                    // 只撤销旧 candidate 的写入资格；terminal/VM 证明仍由原 AST/CS snapshot 管理。
                    privateSelectedCandidates.remove(previous)
                    withdrawnCandidates += previous
                }
            return
        }
        val instance = formalInstantiation(scope, candidate)
        val site = instance.key.callSlot()
        val alternatives = formalAllocations.filter { (other, allocation) ->
            other !== candidate && allocation.key.callSlot() == site
        }.keys.toList()
        val replacedSelection = alternatives.any { it in formalSelectedAllocations }
        for (other in alternatives) {
            formalSelectedAllocations.remove(other)
            formalAllocations.remove(other)
            withdrawnCandidates += other
        }
        if (replacedSelection) {
            formalOutcomes.remove(site)
            completedFormalBindings.remove(site)
        }
        formalSelectedAllocations[candidate] = instance
    }

    /** 实际 body producer 撤销旧 candidate 的写入资格；terminal 和 VM 仍由原语义快照管理。 */
    internal fun withdrawCompletionCandidate(scope: CfirImplicitTypeComputationScope, candidate: Candidate) {
        check(scope.graph === this && candidate.implicitTypeComputationScope?.graph === this)
        if (isCandidateWithdrawn(candidate)) return
        check(!retired && registry[resource] === this)
        withdrawCandidateWriterBindings(candidate)
    }

    /** 撤权只改变实际 candidate 的 selection/writer capability；CS、VM 与 terminal 事实由语义状态持有。 */
    private fun withdrawCandidateWriterBindings(candidate: Candidate) {
        withdrawnCandidates += candidate
        privateSelectedCandidates.remove(candidate)
        formalSelectedAllocations.remove(candidate)
        // 只撤销该 candidate 的绑定，不擦除 shared source slot 上其他新 producer 的权限。
        formalWriterBindings.values.forEach { it.remove(candidate) }
    }

    /** 失败时只报告实际对象在已登记 source/clone/derived 域中的身份，不按源码或名称寻找替代节点。 */
    private fun describeCallSiteProvenance(actual: CfirElement, canonical: CfirElement): String {
        fun identity(element: CfirElement?): String = if (element == null) "absent" else
            "${element.javaClass.simpleName}@${System.identityHashCode(element).toString(16)}"
        val knownOwners = registry.values.filter { graph ->
            actual in graph.cloneToSource || actual in graph.derivedSites || actual in graph.canonicalToPrivate
        }.joinToString { graph ->
            "graph@${System.identityHashCode(graph).toString(16)}(" +
                    "rawSource=${actual in graph.canonicalToPrivate}," +
                    "cloneSource=${identity(graph.cloneToSource[actual])}," +
                    "derivedSource=${identity(graph.derivedSites[actual])})"
        }
        return "graph@${System.identityHashCode(this).toString(16)}, " +
                "actual=${identity(actual)}, canonical=${identity(canonical)}, " +
                "actualIsRawSource=${actual in canonicalToPrivate}, " +
                "actualCloneSource=${identity(cloneToSource[actual])}, " +
                "actualDerivedSource=${identity(derivedSites[actual])}, knownOwners=[$knownOwners]"
    }

    private fun CfirImplicitTypeComputationScope.key(candidate: Candidate): CandidateKey {
        val site = canonicalElement(candidate.callInfo.callSite)
        return CandidateKey(site, target(candidate), candidate.callInfo.callKind, candidate.callInfo.origin,
            candidate.callInfo.implicitInvokeMode, candidate.lookupProvenance)
    }

    fun freshVariables(
        scope: CfirImplicitTypeComputationScope,
        candidate: Candidate,
        parameters: List<CfirTypeParameterRef>,
        create: () -> List<ConeTypeParameterBasedTypeVariable>,
    ): List<ConeTypeParameterBasedTypeVariable> {
        val parameterSymbols = parameters.map { scope.canonicalSymbol(it.symbol) as CfirTypeParameterSymbol }
        val instantiation = when (scope.mode) {
            CfirImplicitTypeComputationMode.PRIVATE -> privateInstantiation(scope, candidate)
            CfirImplicitTypeComputationMode.FORMAL -> formalInstantiation(scope, candidate)
        }
        val retained = instantiation.variables
        if (retained != null) {
            val previous = checkNotNull(instantiation.parameters)
            check(previous.size == parameterSymbols.size && previous.zip(parameterSymbols).all { (a, b) -> a === b }) {
                "Formal candidate changed the declaration parameters of a retained instantiation"
            }
            return retained
        }
        return create().also {
            instantiation.parameters = parameterSymbols
            instantiation.variables = it.toList()
        }
    }

    private fun privateInstantiation(scope: CfirImplicitTypeComputationScope, candidate: Candidate): Instantiation =
        privateInstantiations.getOrPut(candidate) {
            val key = scope.key(candidate)
            Instantiation(key).also { fresh ->
                selectedInstantiations[key]?.takeIf {
                    it.outcome == CfirRetainedCallOutcome.PARTIAL || it.outcome == CfirRetainedCallOutcome.COMPLETED
                }?.let { retained ->
                    fresh.parameters = retained.parameters
                    fresh.variables = retained.variables
                    fresh.associatedVariables.putAll(retained.associatedVariables)
                }
            }
        }

    /** 无 candidate 的正式错误仍有真实源码结果，不能靠 CandidateKey miss 隐去拒绝。 */
    fun recordRejectedSourceCall(scope: CfirImplicitTypeComputationScope, call: CfirElement) {
        if (scope.mode != CfirImplicitTypeComputationMode.FORMAL) return
        check(call is CfirResolvable && call.calleeReference is CfirDiagnosticHolder)
        val source = scope.canonicalExpressionSlotOwner(call) ?: return
        formalSourceRejections += source
    }

    /** 跳过、部分完成和错误都保存实际试算；它们不能建立成功终选义务。 */
    fun recordCallAttempt(scope: CfirImplicitTypeComputationScope, candidate: Candidate, outcome: CfirRetainedCallOutcome) {
        check(outcome != CfirRetainedCallOutcome.COMPLETED)
        check(candidate !in withdrawnCandidates) { "A withdrawn candidate was reused for a new attempt" }
        selectCompletionCandidate(scope, candidate)
        // 同一 Candidate 再次探索也不能改写 terminal 持有的可变实例。
        if (scope.mode == CfirImplicitTypeComputationMode.PRIVATE) {
            privateInstantiations[candidate]?.takeIf { it.outcome == CfirRetainedCallOutcome.COMPLETED }?.let { old ->
                privateInstantiations[candidate] = Instantiation(old.key).also { fresh ->
                    fresh.parameters = old.parameters
                    fresh.variables = old.variables
                    fresh.associatedVariables.putAll(old.associatedVariables)
                }
            }
        }
        val instantiation = when (scope.mode) {
            CfirImplicitTypeComputationMode.PRIVATE -> privateInstantiation(scope, candidate)
            CfirImplicitTypeComputationMode.FORMAL -> formalInstantiation(scope, candidate)
        }
        if (scope.mode == CfirImplicitTypeComputationMode.PRIVATE) {
            instantiation.outcome = outcome
            instantiation.terminalTarget = null
            instantiation.storage = candidate.system.captureStorage()
            privateSelectedCandidates += candidate
            // 探索不能撤销已提交的终选；撤销只由对应 AST/CS/cache snapshot 完成。
            if (selectedInstantiations[instantiation.key]?.outcome != CfirRetainedCallOutcome.COMPLETED) {
                selectedInstantiations[instantiation.key] = instantiation
            }
        } else {
            formalOutcomes[instantiation.key.callSlot()] = outcome
            completedFormalBindings.remove(instantiation.key.callSlot())
        }
    }

    private val completionLeases = Collections.newSetFromMap(IdentityHashMap<CfirRetainedCompletionLease, Boolean>())
    private var retired = false

    /** writer 创建时取得源码域凭证；它与同一 AST/CS/cache snapshot 一起撤销或恢复。 */
    fun captureCompletionLease(
        scope: CfirImplicitTypeComputationScope,
        root: CfirElement,
        system: ConstraintSystemImpl,
    ): CfirRetainedCompletionLease {
        val sources = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
        root.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                scope.canonicalExpressionSlotOwner(element)?.let { sources += it }
                element.acceptChildren(this)
            }
        })
        check(!retired && registry[resource] === this) { "Completion writer belongs to a retired or unregistered resource graph" }
        check(sources.isNotEmpty()) { "Completion writer has no source ownership in this graph" }
        // 新 writer 取得该源码域的写入资格；父 writer 未受影响的源码槽继续有效。
        completionLeases.toList().forEach { previous ->
            if (previous.mode == scope.mode) {
                previous.grantedSources.removeAll(sources)
            }
            if (previous.mode == scope.mode) {
                formalWriterBindings[previous]?.entries?.removeAll { it.value.instantiation.key.callSite in sources }
            }
            if (previous.grantedSources.isEmpty()) {
                completionLeases.remove(previous)
                formalWriterBindings.remove(previous)
            }
        }
        return CfirRetainedCompletionLease(this, sources, system, scope.mode).also { lease ->
            completionLeases += lease
            if (scope.mode == CfirImplicitTypeComputationMode.FORMAL) {
                formalSelectedAllocations.keys.toList().filter { scope.key(it).callSite in sources }.forEach { candidate ->
                    bindFormalCompletionToLease(scope, candidate, lease)
                }
            }
        }
    }

    /**
     * 实际 FORMAL carrier 选择与 writer capability 一起建立代次绑定。
     * 相同 VM 只允许保留分配槽；只有后续真实 writer 成功才建立该 terminal 的消费证明。
     * 已创建 writer 的 binding 不会因 PRIVATE 的后到发布而自动迁移。
     */
    internal fun bindFormalCompletionToLease(
        scope: CfirImplicitTypeComputationScope,
        candidate: Candidate,
        lease: CfirRetainedCompletionLease,
    ) {
        check(scope.mode == CfirImplicitTypeComputationMode.FORMAL && isCompletionLeaseActive(scope, candidate, lease))
        val bindings = formalWriterBindings.getOrPut(lease) { IdentityHashMap() }
        if (candidate in bindings) {
            return
        }
        val allocation = checkNotNull(formalSelectedAllocations[candidate]) { "Formal writer has no actual selected allocation" }
        val key = scope.key(candidate)
        val terminal = terminals[key.callSlot()]
        val bound = if (terminal == null) allocation else {
            val retained = terminal.instantiation
            check(key == retained.key) { "Formal writer changed retained declaration/provenance" }
            fun <T : Any> sameIdentities(first: List<T>?, second: List<T>?): Boolean =
                if (first == null || second == null) first == null && second == null
                else first.size == second.size && first.zip(second).all { (a, b) -> a === b }
            check(sameIdentities(allocation.parameters, retained.parameters) &&
                sameIdentities(allocation.variables, retained.variables) &&
                allocation.associatedVariables.keys == retained.associatedVariables.keys &&
                allocation.associatedVariables.all { (slot, variable) -> retained.associatedVariables[slot] === variable }) {
                "Formal writer cannot bind a terminal allocated by another VM transaction"
            }
            // 新 writer 可显式绑定后到 terminal；没有写回、固定解或完成状态在此被制造。
            formalAllocations[candidate] = retained
            formalSelectedAllocations[candidate] = retained
            retained
        }
        bindings[candidate] = FormalCompletionBinding(terminal?.generation ?: Generation(), bound, terminal)
    }

    fun isCompletionLeaseActive(scope: CfirImplicitTypeComputationScope, candidate: Candidate, lease: CfirRetainedCompletionLease): Boolean =
        !retired && lease.graph === this && registry[resource] === this && lease.mode == scope.mode && lease in completionLeases &&
            scope.key(candidate).callSite in lease.grantedSources && candidate !in withdrawnCandidates

    fun isCandidateWithdrawn(candidate: Candidate): Boolean = retired || registry[resource] !== this || candidate in withdrawnCandidates

    /** 当前 source actual selection 的可变任务；PRIVATE 实例状态与 FORMAL 当前 outcome 分别核对。 */
    internal fun isMutableSourceCallSelected(
        scope: CfirImplicitTypeComputationScope,
        candidate: Candidate,
        call: CfirExpression,
    ): Boolean {
        if (scope.graph !== this || isCandidateWithdrawn(candidate)) return false
        val key = scope.key(candidate)
        if (scope.canonicalExpressionSlotOwner(call) !== key.callSite) return false
        return when (scope.mode) {
            CfirImplicitTypeComputationMode.PRIVATE -> {
                val instance = privateInstantiations[candidate] ?: return false
                candidate in privateSelectedCandidates && instance.key == key &&
                    instance.outcome != CfirRetainedCallOutcome.COMPLETED
            }
            CfirImplicitTypeComputationMode.FORMAL -> {
                val allocation = formalSelectedAllocations[candidate] ?: return false
                allocation.key == key && formalOutcomes[key.callSlot()] in setOf(
                    CfirRetainedCallOutcome.SKIPPED, CfirRetainedCallOutcome.PARTIAL, CfirRetainedCallOutcome.REJECTED,
                )
            }
        }
    }

    /** 已执行的 source terminal/binding 只读消费；不创建 lease、分配实例或登记新的 replay。 */
    internal fun hasCompletedSourceCallResult(call: CfirExpression): Boolean {
        if (retired || registry[resource] !== this) return false
        val reference = (call as? CfirResolvable)?.calleeReference ?: return false
        if (!reference.isSuccessfulResolvedCallReference()) return false
        val type = call.coneTypeOrNull ?: return false
        var formalResult = false
        collectCompletedFormalCallCarriers(setOf(call)) { carrier, _ ->
            if (carrier === call) formalResult = true
        }
        if (formalResult) return true
        return privateSelectedCandidates.any { candidate ->
            if (isCandidateWithdrawn(candidate)) return@any false
            val scope = candidate.implicitTypeComputationScope ?: return@any false
            if (scope.graph !== this || scope.mode != CfirImplicitTypeComputationMode.PRIVATE) return@any false
            val instance = privateInstantiations[candidate] ?: return@any false
            val terminal = terminals[instance.key.callSlot()] ?: return@any false
            terminal.sourceGeneration === generation && terminal.instantiation === instance &&
                terminal.candidate === candidate && terminal.carrier === call &&
                terminal.reference === reference && terminal.type === type &&
                selectedInstantiations[instance.key] === instance && instance.key == scope.key(candidate) &&
                instance.outcome == CfirRetainedCallOutcome.COMPLETED && instance.terminalTarget == instance.key.target &&
                matchesResolvedCompletionReference(scope, candidate, call, reference) &&
                candidate.hasSuccessfulSourceResult(type)
        }
    }

    /** 已 resolved 的调用也必须使用显式 completion atom 的同一语义目标。 */
    fun matchesResolvedCompletionTarget(scope: CfirImplicitTypeComputationScope, candidate: Candidate, symbol: CfirBasedSymbol<*>): Boolean {
        val actual = syntheticTargets[symbol] ?: CallTarget.Declaration(
            canonicalSymbol((symbol as? CfirCallableSymbol<*>)?.unwrapSubstitutionOverrides() ?: symbol),
        )
        return actual == scope.target(candidate)
    }

    fun matchesResolvedCompletionReference(
        scope: CfirImplicitTypeComputationScope,
        candidate: Candidate,
        call: CfirExpression,
        reference: CfirReference,
    ): Boolean = when (reference) {
        is CfirResolvedNamedReference -> reference !is CfirDiagnosticHolder &&
            matchesResolvedCompletionTarget(scope, candidate, reference.resolvedSymbol)
        is CfirResolvedBuiltinOperatorReference -> candidate.matchesBuiltinOperatorReference(call, reference)
        else -> false
    }

    private fun CfirReference.isSuccessfulResolvedCallReference(): Boolean =
        this is CfirResolvedBuiltinOperatorReference || this is CfirResolvedNamedReference && this !is CfirDiagnosticHolder

    /**
     * 将已执行 FULL 写回的 canonical FORMAL carrier 交给本次真实 AST writer。
     * 仅消费当前树中同一个节点、仍被选中的同一个候选及其实际 resolved target；
     * 不按名字或源码位置恢复，也不把旧完成代次登记为本次 terminal 的 replay。
     */
    internal fun collectCompletedFormalCallCarriers(
        carriers: Set<CfirElement>,
        collect: (CfirExpression, Candidate) -> Unit,
    ) {
        check(!retired && registry[resource] === this)
        for ((slot, completed) in completedFormalBindings.toMap()) {
            val carrier = completed.carrier
            if (carrier !in carriers) continue
            val candidate = completed.candidate
            if (isCandidateWithdrawn(candidate) || candidate !in formalSelectedAllocations) continue
            val scope = checkNotNull(candidate.implicitTypeComputationScope)
            check(scope.graph === this && scope.mode == CfirImplicitTypeComputationMode.FORMAL)
            if (scope.privateTreeScope() != null) continue
            check(scope.key(candidate).callSlot() == slot &&
                scope.key(candidate) == completed.binding.instantiation.key &&
                scope.canonicalExpressionSlotOwner(carrier) === slot.site &&
                formalOutcomes[slot] == CfirRetainedCallOutcome.COMPLETED &&
                candidate.hasSuccessfulSourceResult(completed.type)
            ) { "Completed formal carrier changed its actual candidate or source ownership" }
            val reference = (carrier as CfirResolvable).calleeReference
            // 未完成的新引用仍由原 AST/atom collector 处理；错误引用不发布成功 carrier。
            if (!reference.isSuccessfulResolvedCallReference()) continue
            check(matchesResolvedCompletionReference(scope, candidate, carrier, reference)) {
                "Completed formal carrier changed its actual resolved declaration"
            }
            // 先核验真实声明；同一声明的新引用或类型仍不能继承旧 binding。
            if (reference !== completed.reference || carrier.coneTypeOrNull !== completed.type) continue
            collect(carrier, candidate)
        }
    }

    /**
     * 本 lease 是否已经实际写回同一 FORMAL carrier。
     * 以当前 memento 管理的 binding 对象判定；新工厂的 binding 不能继承旧 writer 的完成证明。
     */
    internal fun isFormalCallWrittenByLease(
        scope: CfirImplicitTypeComputationScope,
        candidate: Candidate,
        call: CfirExpression,
        lease: CfirRetainedCompletionLease,
    ): Boolean {
        check(scope.mode == CfirImplicitTypeComputationMode.FORMAL && isCompletionLeaseActive(scope, candidate, lease))
        val binding = checkNotNull(formalWriterBindings[lease]?.get(candidate))
        val completed = completedFormalBindings[scope.key(candidate).callSlot()] ?: return false
        return completed.binding === binding && completed.candidate === candidate && completed.carrier === call
    }

    /** 只能由真正的最终 writer 调用；返回 true 表示本次实际写回已通过全部成功验证。 */
    fun recordCompletedCall(
        scope: CfirImplicitTypeComputationScope,
        candidate: Candidate,
        call: CfirExpression,
        system: ConstraintSystemImpl,
        substitutor: ConeSubstitutor,
        lease: CfirRetainedCompletionLease,
    ): Boolean {
        // 不能仅按 Candidate 是否还存在判断 writer 代次；已存在 Candidate 也能被回滚重用。
        if (!isCompletionLeaseActive(scope, candidate, lease)) {
            return false
        }
        check(lease.system === system) { "Completion writer changed its actual CS owner" }
        selectCompletionCandidate(scope, candidate)
        val reference = (call as CfirResolvable).calleeReference
        val type = call.coneTypeOrNull
        if (!candidate.isSuccessful || reference is CfirDiagnosticHolder ||
            reference is CfirNamedReferenceWithCandidate && reference.isError ||
            type == null || type.contains { it is ConeErrorType }
        ) {
            recordCallAttempt(scope, candidate, CfirRetainedCallOutcome.REJECTED)
            return false
        }
        // isSuccessful 只是 applicability；还必须通过最终 writer、atom 和变量替换证明。
        if (!reference.isSuccessfulResolvedCallReference() ||
            type.contains { it is ConeTypeVariableType } || candidate.postponedAtoms.any { !it.analyzed }) {
            recordCallAttempt(scope, candidate, CfirRetainedCallOutcome.PARTIAL)
            return false
        }
        check(matchesResolvedCompletionReference(scope, candidate, call, reference)) {
            "Completion candidate differs from the actual resolved target"
        }
        val formalBinding = if (scope.mode == CfirImplicitTypeComputationMode.FORMAL) {
            checkNotNull(formalWriterBindings[lease]?.get(candidate)) { "Formal completion has no writer-bound terminal generation" }
                .also { binding ->
                    check(formalSelectedAllocations[candidate] === binding.instantiation) {
                        "Formal writer selection changed after its terminal generation was bound"
                    }
                }
        } else null
        val instantiation = when (scope.mode) {
            CfirImplicitTypeComputationMode.PRIVATE -> privateInstantiation(scope, candidate)
            CfirImplicitTypeComputationMode.FORMAL -> formalInstantiation(scope, candidate)
        }
        val storage = system.captureStorage()
        if (!storage.usesOuterCs && (storage.notFixedTypeVariables.isNotEmpty() || storage.constraintsFromAllForkPoints.isNotEmpty())) {
            recordCallAttempt(scope, candidate, CfirRetainedCallOutcome.PARTIAL)
            return false
        }
        val owned = (instantiation.variables.orEmpty() + instantiation.associatedVariables.values + candidate.freshVariables).distinct()
        // 最终 writer 消费实际候选的已完成存储；FREEZED 后不能进入 allTypeVariables 的可变上下文访问器。
        // 候选视图与本次 writer 的 storage 分别取证，后续仍逐个核对真实 VM 和固定绑定。
        val candidateStorage = candidate.system.captureStorage()
        val additional = candidate.additionalCompletionVariables.map { constructor ->
            checkNotNull(candidateStorage.allTypeVariables[constructor]) as ConeTypeVariable
        }
        for (variable in (owned + additional).distinct()) {
            check(storage.allTypeVariables[variable.typeConstructor] === variable) { "Terminal completion changed its actual VM owner" }
            val fixed = storage.fixedTypeVariables[variable.typeConstructor]
            val binding = substitutor.substituteOrSelf(variable.defaultType)
            if (fixed == null || binding.contains { it is ConeTypeVariableType || it is ConeErrorType } ||
                !AbstractTypeChecker.equalTypes(resource.moduleData.session.typeContext,
                    substitutor.substituteOrSelf(fixed as ConeCangJieType), binding)) {
                recordCallAttempt(scope, candidate, CfirRetainedCallOutcome.PARTIAL)
                return false
            }
        }
        val key = scope.key(candidate)
        if (scope.mode == CfirImplicitTypeComputationMode.PRIVATE) {
            privateSelectedCandidates += candidate
            // 已发布实例不可原位改写，否则旧 FORMAL binding 会看到未来的 PRIVATE 字段。
            val published = Instantiation(key).also { completed ->
                completed.parameters = instantiation.parameters
                completed.variables = instantiation.variables
                completed.associatedVariables.putAll(instantiation.associatedVariables)
                completed.outcome = CfirRetainedCallOutcome.COMPLETED
                completed.terminalTarget = key.target
                completed.storage = storage
            }
            privateInstantiations[candidate] = published
            selectedInstantiations[key] = published
            val terminal = Terminal(Generation(), generation, published, candidate, call, reference, type)
            terminals[key.callSlot()] = terminal
        } else {
            val binding = checkNotNull(formalBinding)
            binding.terminal?.let { terminal ->
                check(key == terminal.instantiation.key) { "Formal success changed retained declaration/provenance" }
                check(binding.instantiation === terminal.instantiation && binding.generation === terminal.generation)
                replayedInstantiations += terminal.instantiation
            }
            completedFormalBindings[key.callSlot()] = CompletedFormalCall(binding, candidate, call, reference, type)
            formalOutcomes[key.callSlot()] = CfirRetainedCallOutcome.COMPLETED
            formalSourceRejections.remove(key.callSite)
        }
        return true
    }

    /** 精确 owner/role/index 的真实类型采用；固定状态本身永远不是消费证据。 */
    fun consumeExpressionType(
        scope: CfirImplicitTypeComputationScope,
        owner: CfirElement,
        role: CfirInferenceVariableRole,
        index: Int,
        type: ConeCangJieType,
        actualSystem: ConstraintSystemImpl?,
    ) {
        check(!retired && registry[resource] === this) { "Expression consumer belongs to a retired or unregistered resource graph" }
        val canonicalOwner = scope.canonicalExpressionSlotOwner(owner) ?: return
        val slot = VariableSlot(canonicalOwner, role, index)
        val variable = expressionVariables[slot] ?: return // 此语义操作没有经过本图工厂。
        val accepted = !type.contains { it is ConeErrorType }
        if (accepted) {
            // 移交后只使用保存的实际接收系统；不能再次读取 session 恢复后的 getter。
            val system = checkNotNull(constraintSystemOwner ?: actualSystem) { "Expression consumption has no actual CS owner" }
            val storage = system.captureStorage()
            check(storage.allTypeVariables[variable.typeConstructor] === variable) { "Expression consumption changed VM identity" }
            val substituted = storage.buildCurrentSubstitutor(resource.moduleData.session.typeContext, emptyMap()).asCone()
            val expected = substituted.substituteOrSelf(variable.defaultType)
            val actual = substituted.substituteOrSelf(type)
            if (variable.typeConstructor in storage.fixedTypeVariables) {
                check(AbstractTypeChecker.equalTypes(resource.moduleData.session.typeContext, expected, actual)) {
                    "Adopted expression type disagrees with retained fixed binding"
                }
            } else {
                check(variable.typeConstructor in storage.notFixedTypeVariables)
                check(system.addEqualityConstraintIfCompatible(variable.defaultType, actual, ConeArgumentConstraintPosition(owner))) {
                    "Adopted expression type contradicts its actual retained VM"
                }
            }
        }
        val uses = if (scope.mode == CfirImplicitTypeComputationMode.PRIVATE) privateExpressionUses else formalExpressionUses
        uses[slot] = ExpressionUse(
            if (scope.mode == CfirImplicitTypeComputationMode.FORMAL) privateExpressionUses[slot]?.generation ?: generation else generation,
            type, accepted,
        )
    }

    fun <V : ConeTypeVariable> variable(
        scope: CfirImplicitTypeComputationScope,
        candidate: Candidate?,
        owner: CfirElement,
        role: CfirInferenceVariableRole,
        index: Int,
        create: () -> V,
    ): V {
        val variables = if (candidate == null) expressionVariables else {
            val instantiation = when (scope.mode) {
                CfirImplicitTypeComputationMode.PRIVATE -> privateInstantiation(scope, candidate)
                CfirImplicitTypeComputationMode.FORMAL -> formalInstantiation(scope, candidate)
            }
            instantiation.associatedVariables
        }
        val slot = VariableSlot(scope.canonicalElement(owner), role, index)
        val previous = variables[slot]
        if (previous != null) {
            if (role == CfirInferenceVariableRole.LAMBDA_RETURN ||
                role == CfirInferenceVariableRole.FACTORY_LAMBDA_RETURN ||
                role == CfirInferenceVariableRole.COMMON_LAMBDA_RETURN
            ) {
                check(previous is ConeTypeVariableForLambdaReturnType && previous.argument === owner) {
                    "Lambda return slot must refer to the actual anonymous-function declaration"
                }
            }
            @Suppress("UNCHECKED_CAST")
            return previous as V
        }
        return create().also { variables[slot] = it }
    }

    /** 已选中普通 FULL 调用的存储也归本图；它们不会通过 PCLA partial-call 回调移交。 */
    fun independentStorages(): List<ConstraintStorage> = selectedInstantiations.values
        .mapNotNull { it.storage }.filterNot { it.usesOuterCs }

    fun selectedCandidates(): List<Candidate> = privateSelectedCandidates.toList()

    /** candidate 只重建自己的实例化槽；入口共享系统的前缀仍由实际 inference owner 持有。 */
    fun variablesForFormalCandidate(scope: CfirImplicitTypeComputationScope, candidate: Candidate): List<ConeTypeVariable> {
        check(scope.mode == CfirImplicitTypeComputationMode.FORMAL)
        val instantiation = selectedInstantiations[scope.key(candidate)]?.takeIf {
            it.outcome == CfirRetainedCallOutcome.PARTIAL || it.outcome == CfirRetainedCallOutcome.COMPLETED
        } ?: return emptyList()
        return instantiation.variables.orEmpty() + instantiation.associatedVariables.values
    }

    /**
     * 与 AST/cache 捕获同一语法子树的 retained 状态。CS 由调用者的真实 transaction 恢复；
     * reinfer 保留已登记在传入系统的 VM，但清除旧轮次的终选与类型采用证明。
     */
    fun captureReplayState(
        scope: CfirImplicitTypeComputationScope,
        root: CfirElement,
        preservedCallSite: CfirElement? = null,
    ): CfirImplicitTypeReplaySnapshot {
        val preservedSource = preservedCallSite?.let(scope::canonicalExpressionSlotOwner)
        val activeRootSource = scope.canonicalExpressionSlotOwner(root)
        fun sources(): Set<CfirElement> {
            val result = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
            root.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    scope.canonicalExpressionSlotOwner(element)?.let { result += it }
                    element.acceptChildren(this)
                }
            })
            if (preservedSource != null) result.remove(preservedSource)
            return result
        }
        val beforeSources = sources()
        val beforeGeneration = generation
        val beforeRetired = retired
        val beforeResultTypeRef = resultTypeRef
        val beforeSourcesTasks = formalResourceSources.toList()
        // canonical declaration 可以位于 private initializer 之外，仍是本图 source callback 的实际写入槽。
        val sourceResources = Collections.newSetFromMap(IdentityHashMap<CfirResourceVariable, Boolean>()).apply {
            add(resource)
            beforeSourcesTasks.filter { it.isCurrent }.forEach { add(it.resource) }
        }
        val beforeSourceSnapshots = sourceResources.map { CfirResourceSourceSnapshot.capture(it) }
        val privateResourceStates = captureResourceSourceStates(root)
        val beforeLeases = IdentityHashMap<CfirRetainedCompletionLease, Set<CfirElement>>().apply {
            completionLeases.forEach { lease ->
                val grants = lease.grantedSources.filterTo(Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())) { it in beforeSources }
                if (grants.isNotEmpty()) put(lease, grants)
            }
        }
        val beforeWriterBindings = IdentityHashMap<CfirRetainedCompletionLease, IdentityHashMap<Candidate, FormalCompletionBinding>>().apply {
            formalWriterBindings.forEach { (lease, bindings) ->
                val owned = bindings.filterTo(IdentityHashMap()) { it.value.instantiation.key.callSite in beforeSources }
                if (owned.isNotEmpty()) put(lease, owned)
            }
        }
        val beforeProducerOwner = completionOwnerCandidate
        val beforeProducerSession = inferenceOwner
        val beforeProducerCalls = if (beforeProducerOwner == null) emptyList() else {
            beforeProducerOwner.postponedPCLACalls.mapIndexedNotNull { index, call ->
                val candidate = (call as? org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate)?.candidate
                    ?: return@mapIndexedNotNull null
                val callScope = candidate.implicitTypeComputationScope
                if (callScope?.graph === this && callScope.mode == CfirImplicitTypeComputationMode.PRIVATE &&
                    callScope.key(candidate).callSite in beforeSources &&
                    ownsRegisteredPrivateCallProducer(beforeProducerOwner, call)) {
                    Triple(index, call, checkNotNull(beforeProducerOwner.pclaCallWriters[call])) to
                        checkNotNull(beforeProducerSession).isRetainedCallAlreadyProcessed(beforeProducerOwner, index)
                } else null
            }
        }
        val beforePCLA = IdentityHashMap<Candidate, CandidatePCLACompletionState>().apply {
            privateInstantiations.forEach { (candidate, instance) ->
                if (instance.key.callSite in beforeSources) put(candidate, candidate.capturePCLACompletionState())
            }
            formalAllocations.forEach { (candidate, instance) ->
                if (instance.key.callSite in beforeSources) put(candidate, candidate.capturePCLACompletionState())
            }
        }
        val beforeOrigins = origins.toList()
        val beforeOriginRestores = beforeOrigins.map { binding ->
            val target = binding.current()
            val restore: () -> Unit = { binding.relocate(target) }
            restore
        }
        val beforeOriginDepth = privateOriginDepth
        val beforePrivateContext = privateInitializerContext
        val beforePrivateRootOwner = privateInitializerOwner
        val beforePrivateFlow = beforePrivateContext?.captureDataFlowAnalyzerContext()
        // 只映射本 private initializer 的实际 CFG owner；借用的外部声明不会被 visitor 展开。
        val beforePrivateGraphs = mutableListOf<Pair<
            org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner,
            org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph,
        >>()
        if (beforePrivateFlow != null) {
            initializer.accept(object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    if (element is org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner) {
                        val reference = element.controlFlowGraphReference as?
                            org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
                        val graph = reference?.controlFlowGraph
                        if (graph != null && beforePrivateFlow.graphMapping.containsKey(graph)) {
                            beforePrivateGraphs += element to graph
                        }
                    }
                    element.acceptChildren(this)
                }
            })
        }
        val beforeOwner = inferenceOwner
        val beforeSystemOwner = constraintSystemOwner
        val beforeCompletionOwner = completionOwnerCandidate
        val beforeWriteCall = writeCall
        val restoreAdditionalResults = capturePrivateAdditionalResultsState()
        val beforeAnalyzer = analyzePostponedAtom
        val beforeCalls = ownCalls
        val beforeCallbacks = ownCallbacks
        val beforeLambdas = ownLambdas
        val beforeProjectionOwners = ownProjectionOwners
        val beforeConstructors = ownVariableConstructors
        val beforeProjectionScope = sourceProjectionScope
        val beforeSourceProjection = hasSourceProjection
        val beforePrivate = privateInstantiations.filterTo(IdentityHashMap()) { it.value.key.callSite in beforeSources }
        val beforeFormal = formalAllocations.filterTo(IdentityHashMap()) { it.value.key.callSite in beforeSources }
        val beforeFormalSelected = formalSelectedAllocations.filterTo(IdentityHashMap()) { it.value.key.callSite in beforeSources }
        val beforeSelected = selectedInstantiations.filterKeys { it.callSite in beforeSources }
        val beforeTerminals = terminals.filterKeys { it.site in beforeSources }
        val beforeOutcomes = formalOutcomes.filterKeys { it.site in beforeSources }
        val beforeCompletedBindings = completedFormalBindings.filterKeys { it.site in beforeSources }
        val beforeSourceRejections = formalSourceRejections.filter { it in beforeSources }
        val beforeVariables = expressionVariables.filterKeys { it.owner in beforeSources }
        val beforePrivateUses = privateExpressionUses.filterKeys { it.owner in beforeSources }
        val beforeFormalUses = formalExpressionUses.filterKeys { it.owner in beforeSources }
        val beforeReplayed = replayedInstantiations.filter { it.key.callSite in beforeSources }
        val beforeCandidates = privateSelectedCandidates.filter { privateInstantiations[it]?.key?.callSite in beforeSources }
        val beforeDerived = derivedSites.filterValues { it in beforeSources }
        val beforeSynthetic = syntheticTargets.filterValues { it.source in beforeSources }
        // 快照保存实例化字段值；不能让后续 writer 修改快照中同一 mutable Instantiation。
        val instances = (beforePrivate.values + beforeFormal.values + beforeSelected.values).toSet()
        val restoreInstances = instances.map { instance ->
            val target = instance.terminalTarget
            val storage = instance.storage
            val outcome = instance.outcome
            val parameters = instance.parameters
            val variables = instance.variables
            val associated = instance.associatedVariables.toMap()
            val restore: () -> Unit = {
                instance.terminalTarget = target
                instance.storage = storage
                instance.outcome = outcome
                instance.parameters = parameters
                instance.variables = variables
                instance.associatedVariables.clear()
                instance.associatedVariables.putAll(associated)
            }
            restore
        }
        val children = registry.filterKeys { it in beforeSources }
        val childSnapshots = children.values.filter { it !== this }.map { child ->
            child.captureReplayState(CfirImplicitTypeComputationScope(child, CfirImplicitTypeComputationMode.PRIVATE), child.initializer)
        }
        return CfirImplicitTypeReplaySnapshot { reinferSystem ->
            val affected = beforeSources + sources()
            beforeSourceSnapshots.forEach { it.validateRestore() }
            privateResourceStates.forEach { it.validateRestore() }
            formalResourceSources.filter { current -> beforeSourcesTasks.none { it === current } }.forEach { it.cancel() }
            formalResourceSources.clear()
            formalResourceSources.addAll(beforeSourcesTasks)
            resultTypeRef = beforeResultTypeRef
            if (reinferSystem == null) {
                beforeSourceSnapshots.forEach { it.restore() }
                restoreResourceSourceStates(root, privateResourceStates)
            } else {
                beforeSourceSnapshots.forEach { it.restoreTypeResolutionState() }
                privateResourceStates.forEach { it.restoreTypeResolutionState() }
            }
            retired = beforeRetired
            completionLeases.toList().forEach { lease ->
                lease.grantedSources.removeAll(affected)
                formalWriterBindings[lease]?.entries?.removeAll { it.value.instantiation.key.callSite in affected }
                if (lease.grantedSources.isEmpty()) {
                    completionLeases.remove(lease)
                    formalWriterBindings.remove(lease)
                }
            }
            if (reinferSystem == null) beforeLeases.forEach { (lease, grants) ->
                lease.grantedSources.addAll(grants)
                completionLeases += lease
            }
            if (reinferSystem == null) beforeWriterBindings.forEach { (lease, bindings) ->
                formalWriterBindings.getOrPut(lease) { IdentityHashMap() }.putAll(bindings)
            }
            beforePCLA.forEach { (candidate, state) -> candidate.restorePCLACompletionState(state) }
            // 只恢复本次 AST 范围的 registered PRIVATE 任务；普通 FIFO 和兄弟资源不被覆盖。
            listOfNotNull(beforeProducerOwner, completionOwnerCandidate).distinct().forEach { owner ->
                val affectedCalls = owner.postponedPCLACalls.filter { call ->
                    val candidate = (call as? org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate)?.candidate
                        ?: return@filter false
                    val callScope = candidate.implicitTypeComputationScope
                    callScope?.graph === this && callScope.mode == CfirImplicitTypeComputationMode.PRIVATE &&
                        callScope.key(candidate).callSite in affected &&
                        (ownCalls + beforeCalls).any { it === call } &&
                        (owner.pclaCallWriters[call] === writeCall || owner.pclaCallWriters[call] === beforeWriteCall)
                }
                val ownerSession = if (owner === beforeProducerOwner) beforeProducerSession else inferenceOwner
                affectedCalls.forEach { checkNotNull(ownerSession).consumeRetainedCallProducer(owner, it) }
            }
            if (reinferSystem == null && beforeProducerOwner != null) {
                beforeProducerCalls.forEach { (position, wasProcessed) ->
                    val (index, call, writer) = position
                    checkNotNull(beforeProducerSession).restoreRetainedCallProducer(
                        beforeProducerOwner, index, call, writer, wasProcessed,
                    )
                }
            }
            val discardedCandidates = Collections.newSetFromMap(IdentityHashMap<Candidate, Boolean>()).apply {
                addAll(privateInstantiations.filterTo(IdentityHashMap()) { it.value.key.callSite in affected }.keys)
                addAll(formalAllocations.filterTo(IdentityHashMap()) { it.value.key.callSite in affected }.keys)
            }
            // enclosing overload/synthetic candidate 本身仍由调用者恢复并采用；撤销其 body 中的新调用。
            withdrawnCandidates.addAll(discardedCandidates.filter { candidate ->
                scope.canonicalExpressionSlotOwner(candidate.callInfo.callSite) !== activeRootSource
            })
            // 普通探索候选可随语义 rollback 恢复；成功重查已替换的 source producer 不可复活。
            withdrawnCandidates.removeAll { !it.isSourceCompletionWithdrawn && (it in beforePrivate || it in beforeFormal) }
            val survivingExpressions = if (reinferSystem == null) emptyMap() else expressionVariables.filter { (slot, variable) ->
                slot.owner in affected && reinferSystem.allTypeVariables[variable.typeConstructor] === variable
            }
            val survivingInstances = if (reinferSystem == null) emptyList() else selectedInstantiations.values.filter { instance ->
                instance.key.callSite in affected &&
                        (instance.outcome == CfirRetainedCallOutcome.PARTIAL || instance.outcome == CfirRetainedCallOutcome.COMPLETED) &&
                        (instance.variables.orEmpty() + instance.associatedVariables.values).isNotEmpty() &&
                        (instance.variables.orEmpty() + instance.associatedVariables.values).all { variable ->
                            reinferSystem.allTypeVariables[variable.typeConstructor] === variable
                        }
            }
            privateInstantiations.entries.removeAll { it.value.key.callSite in affected }
            privateInstantiations.putAll(beforePrivate)
            formalAllocations.entries.removeAll { it.value.key.callSite in affected }
            formalAllocations.putAll(beforeFormal)
            formalSelectedAllocations.entries.removeAll { it.value.key.callSite in affected }
            formalSelectedAllocations.putAll(beforeFormalSelected)
            selectedInstantiations.keys.removeAll { it.callSite in affected }
            selectedInstantiations.putAll(beforeSelected)
            terminals.keys.removeAll { it.site in affected }
            terminals.putAll(beforeTerminals)
            formalOutcomes.keys.removeAll { it.site in affected }
            formalOutcomes.putAll(beforeOutcomes)
            completedFormalBindings.keys.removeAll { it.site in affected }
            completedFormalBindings.putAll(beforeCompletedBindings)
            formalSourceRejections.removeAll { it in affected }
            formalSourceRejections.addAll(beforeSourceRejections)
            expressionVariables.keys.removeAll { it.owner in affected }
            expressionVariables.putAll(beforeVariables)
            privateExpressionUses.keys.removeAll { it.owner in affected }
            privateExpressionUses.putAll(beforePrivateUses)
            formalExpressionUses.keys.removeAll { it.owner in affected }
            formalExpressionUses.putAll(beforeFormalUses)
            replayedInstantiations.removeAll { it.key.callSite in affected }
            replayedInstantiations.addAll(beforeReplayed)
            privateSelectedCandidates.removeAll { it in discardedCandidates }
            privateSelectedCandidates.addAll(beforeCandidates)
            derivedSites.entries.removeAll { it.value in affected }
            derivedSites.putAll(beforeDerived)
            syntheticTargets.entries.removeAll { it.value.source in affected }
            syntheticTargets.putAll(beforeSynthetic)
            restoreInstances.forEach { it() }
            check(privateOriginDepth == beforeOriginDepth) { "Retained replay changed active origin stack" }
            // PRIVATE 根的结构状态随两类重放恢复；reinfer 的实际 CS 和 active AST 保持原有协议。
            privateInitializerContext = beforePrivateContext
            privateInitializerOwner = beforePrivateRootOwner
            if (beforePrivateFlow != null) {
                checkNotNull(beforePrivateRootOwner)
                checkNotNull(beforePrivateContext).restoreDataFlowAnalyzerContext(beforePrivateFlow)
                beforePrivateGraphs.forEach { (owner, graph) ->
                    owner.replaceControlFlowGraphReference(
                        org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(
                            checkNotNull(beforePrivateFlow.graphMapping[graph]),
                        ),
                    )
                }
            }
            if (reinferSystem == null) {
                origins.clear()
                origins.addAll(beforeOrigins)
                beforeOriginRestores.forEach { it() }
                ownVariableConstructors = beforeConstructors
                inferenceOwner = beforeOwner
                constraintSystemOwner = beforeSystemOwner
                completionOwnerCandidate = beforeCompletionOwner
                writeCall = beforeWriteCall
                analyzePostponedAtom = beforeAnalyzer
                ownCalls = beforeCalls
                ownCallbacks = beforeCallbacks
                ownLambdas = beforeLambdas
                ownProjectionOwners = beforeProjectionOwners
                sourceProjectionScope = beforeProjectionScope
                hasSourceProjection = beforeSourceProjection
            } else {
                // reinfer 的 CS 解继续存活；旧 AST writer/回调由其 candidate 重建。
                ownCalls = beforeCalls
                ownCallbacks = beforeCallbacks
                ownLambdas = beforeLambdas
                ownProjectionOwners = beforeProjectionOwners
            }
            restoreAdditionalResults(if (reinferSystem == null) null else completionOwnerCandidate)
            // 只撤销被本次 AST/CS 边界移除或替换的 graph 的 source 回调，不触碰存活的兄弟图。
            registry.values.toList().filter { it.resource in affected && children[it.resource] !== it && it !== this }
                .forEach { discarded -> discarded.cancelResourceSourceCallbacks() }
            registry.keys.removeAll { it in affected && it !in children }
            registry.putAll(children)
            childSnapshots.forEach { it.restoreForRollback() }
            generation = if (reinferSystem == null) beforeGeneration else Generation()
            if (reinferSystem != null) {
                expressionVariables.putAll(survivingExpressions)
                for (instance in survivingInstances) {
                    if (instance.key in beforeSelected) continue
                    instance.terminalTarget = null
                    instance.outcome = CfirRetainedCallOutcome.PARTIAL
                    instance.storage = reinferSystem.captureStorage()
                    selectedInstantiations[instance.key] = instance
                }
            }
            // memento 可恢复 allocation/VM/terminal，但不能重新授权已永久撤权的实际 source candidate。
            val restoredCandidates = Collections.newSetFromMap(IdentityHashMap<Candidate, Boolean>()).apply {
                addAll(privateInstantiations.keys)
                addAll(formalAllocations.keys)
                addAll(withdrawnCandidates)
                formalWriterBindings.values.forEach { addAll(it.keys) }
            }
            restoredCandidates.filter { it.isSourceCompletionWithdrawn }.forEach(::withdrawCandidateWriterBindings)
        }
    }

    private var defaultFinalStorage: ConstraintStorage? = null

    /** DEFAULT 的外层 FULL 调用完成内层 partial 后，以整个闭合系统保存最终绑定。 */
    fun retainDefaultFinalStorage(storage: ConstraintStorage, actualOwner: ConstraintSystemImpl) {
        check(!storage.usesOuterCs && storage.notFixedTypeVariables.isEmpty())
        val actual = actualOwner.captureStorage()
        check(actual.allTypeVariables.keys == storage.allTypeVariables.keys &&
            storage.allTypeVariables.all { (key, variable) -> actual.allTypeVariables[key] === variable } &&
            actual.fixedTypeVariables == storage.fixedTypeVariables) { "Default final storage changed its actual CS owner" }
        defaultFinalStorage = storage
        constraintSystemOwner = actualOwner
    }

    fun storageForFormalCandidate(scope: CfirImplicitTypeComputationScope, candidate: Candidate): ConstraintStorage? {
        check(scope.mode == CfirImplicitTypeComputationMode.FORMAL)
        val instantiation = selectedInstantiations[scope.key(candidate)]?.takeIf {
            it.outcome == CfirRetainedCallOutcome.PARTIAL || it.outcome == CfirRetainedCallOutcome.COMPLETED
        } ?: return null
        val finalStorage = defaultFinalStorage
        return if (finalStorage != null && finalStorage.allTypeVariables.keys.containsAll(
                checkNotNull(instantiation.storage).allTypeVariables.keys,
            )) finalStorage else instantiation.storage
    }

    var resultTypeRef: CfirResolvedTypeRef? = null
        private set

    internal var ownCalls: List<ConeResolutionAtom> = emptyList()
        private set
    internal var ownCallbacks: List<(ConeSubstitutor) -> Unit> = emptyList()
        private set
    internal var ownLambdas: List<CfirDeclaration> = emptyList()
        private set
    internal var ownProjectionOwners: List<CfirElement> = emptyList()
        private set
    internal var inferenceOwner: CfirPCLAInferenceSession? = null
    /** 移交时实际接收约束的可变系统；session 恢复旧 common-system 视图不改变这份 authority。 */
    internal var constraintSystemOwner: ConstraintSystemImpl? = null
    /** resource 入口的实际 parent 基线；仅为 CS seam，不参与 terminal/expression 决议。 */
    internal var parentConstraintBaseline: org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemBaseline? = null
    internal var writeCall: ((ConeResolutionAtom, ConeSubstitutor) -> Unit)? = null
    internal var completionCallback: ((ConeSubstitutor) -> Unit)? = null
    internal var completionOwnerCandidate: Candidate? = null
    internal var analyzePostponedAtom: ((ConstraintSystemImpl, ConePostponedResolvedAtom, Candidate, Boolean) -> Unit)? = null

    private var privateAdditionalResultsWriter:
            ((List<(ConeSubstitutor) -> Unit>, List<CfirDeclaration>, ConeSubstitutor) -> Unit)? = null
    private var privateAdditionalResultsOwner: Candidate? = null
    /** dispatcher capability 不进入 memento；工作 revision 与完成事实进入同一语义快照。 */
    private var privateAdditionalResultsEpoch: Any = Any()
    private var privateAdditionalResultsRevision: Any = Any()
    private val completedPrivateAdditionalCallbacks =
        Collections.newSetFromMap(IdentityHashMap<(ConeSubstitutor) -> Unit, Boolean>())

    private class PrivateAdditionalResultsCompletion(
        val writer: (List<(ConeSubstitutor) -> Unit>, List<CfirDeclaration>, ConeSubstitutor) -> Unit,
        val owner: Candidate,
        val system: ConstraintSystemImpl,
        val storage: ConstraintStorage,
        val revision: Any,
        val callbacks: List<(ConeSubstitutor) -> Unit>,
        val lambdas: List<CfirDeclaration>,
    )
    private var privateAdditionalResultsCompletion: PrivateAdditionalResultsCompletion? = null

    /** raw writer 保留真实 PRIVATE lexical/DFA；注册对象只代表执行任务，不代表完成。 */
    internal fun registerPrivateAdditionalResultsProducer(
        owner: Candidate,
        writer: (List<(ConeSubstitutor) -> Unit>, List<CfirDeclaration>, ConeSubstitutor) -> Unit,
    ): CfirRetainedAdditionalResultsProducer {
        check(!retired && registry[resource] === this && privateAdditionalResultsWriter == null)
        privateAdditionalResultsWriter = writer
        privateAdditionalResultsOwner = owner
        privateAdditionalResultsEpoch = Any()
        privateAdditionalResultsCompletion = null
        completedPrivateAdditionalCallbacks.clear()
        return CfirRetainedAdditionalResultsProducer(this, owner, privateAdditionalResultsEpoch).also {
            completionCallback = it
        }
    }

    /** 只撤销本图 dispatcher，Source、ordinary callbacks 与兄弟图仍保留原对象/相对顺序。 */
    private fun removePrivateAdditionalDispatchers(owner: Candidate) {
        owner.onPCLACompletionResultsWritingCallbacks.removeAll {
            it is CfirRetainedAdditionalResultsProducer && it.graph === this
        }
    }

    private fun queuePrivateAdditionalResultsProducer(owner: Candidate): CfirRetainedAdditionalResultsProducer {
        val callbacks = owner.onPCLACompletionResultsWritingCallbacks
        val index = callbacks.indexOfFirst { it is CfirRetainedAdditionalResultsProducer && it.graph === this }
        removePrivateAdditionalDispatchers(owner)
        val producer = CfirRetainedAdditionalResultsProducer(this, owner, privateAdditionalResultsEpoch)
        completionCallback = producer
        if (index < 0) callbacks += producer else callbacks.add(index, producer)
        return producer
    }

    /** 只在真实 child 移交或 private analyzer 的实际 owner 变化后改绑，旧 handle 不复活。 */
    internal fun rebindPrivateAdditionalResultsProducerOwner(owner: Candidate) {
        if (privateAdditionalResultsWriter == null) return
        check(!retired && registry[resource] === this && completionOwnerCandidate === owner)
        val previousOwner = checkNotNull(privateAdditionalResultsOwner)
        if (previousOwner === owner) return
        removePrivateAdditionalDispatchers(previousOwner)
        privateAdditionalResultsOwner = owner
        privateAdditionalResultsEpoch = Any()
        privateAdditionalResultsCompletion = null
        completedPrivateAdditionalCallbacks.clear()
        // 新 actual owner 的 declaration/lambda 投影都须重新执行；旧完成事实不能冒充新系统写回。
        queuePrivateAdditionalResultsProducer(owner)
    }

    internal fun ownsRegisteredPrivateAdditionalResultsProducer(
        owner: Candidate,
        producer: CfirRetainedAdditionalResultsProducer,
    ): Boolean =
        !retired && registry[resource] === this && producer.graph === this && producer.owner === owner &&
            completionOwnerCandidate === owner && privateAdditionalResultsOwner === owner &&
            inferenceOwner?.isOuterCandidate(owner) == true && owner.retainedChildSessions.any { it === this } &&
            privateAdditionalResultsWriter != null && completionCallback === producer &&
            producer.epoch === privateAdditionalResultsEpoch &&
            owner.onPCLACompletionResultsWritingCallbacks.any { it === producer }

    private fun privateAdditionalResultsAreComplete(owner: Candidate, system: ConstraintSystemImpl): Boolean {
        val fact = privateAdditionalResultsCompletion ?: return false
        if (fact.writer !== privateAdditionalResultsWriter || fact.owner !== owner || fact.system !== system ||
            fact.revision !== privateAdditionalResultsRevision ||
            fact.callbacks.size != ownCallbacks.size || fact.callbacks.zip(ownCallbacks).any { (a, b) -> a !== b } ||
            fact.lambdas.size != ownLambdas.size || fact.lambdas.zip(ownLambdas).any { (a, b) -> a !== b }) return false
        val actual = system.captureStorage()
        val before = fact.storage
        // 兄弟图后来追加的变量不撤销既有写回；本次执行所读的 VM、固定解与 outer/fork 边界仍必须相同。
        return actual.allTypeVariables.keys.take(before.allTypeVariables.size) == before.allTypeVariables.keys.toList() &&
            before.allTypeVariables.all { (constructor, variable) -> actual.allTypeVariables[constructor] === variable } &&
            before.fixedTypeVariables.all { (constructor, type) -> actual.fixedTypeVariables[constructor] == type } &&
            before.notFixedTypeVariables.keys == actual.notFixedTypeVariables.keys.intersect(before.allTypeVariables.keys) &&
            before.hasContradiction == actual.hasContradiction &&
            before.constraintsFromAllForkPoints == actual.constraintsFromAllForkPoints &&
            before.usesOuterCs == actual.usesOuterCs &&
            before.outerSystemVariablesPrefixSize == actual.outerSystemVariablesPrefixSize
    }

    internal fun needsPrivateAdditionalResultsCompletion(owner: Candidate, system: ConstraintSystemImpl): Boolean =
        privateAdditionalResultsWriter != null && completionOwnerCandidate === owner &&
            (!privateAdditionalResultsAreComplete(owner, system) ||
                owner.onPCLACompletionResultsWritingCallbacks.any {
                    it is CfirRetainedAdditionalResultsProducer && it.graph === this
                })

    /** factory 的最终边界恢复/移交后只为本图重新登记有效 handle，不能从空列表制造完成。 */
    internal fun ensurePrivateAdditionalResultsProducer(owner: Candidate, system: ConstraintSystemImpl) {
        if (privateAdditionalResultsWriter == null) return
        check(!retired && registry[resource] === this && completionOwnerCandidate === owner &&
            privateAdditionalResultsOwner === owner && inferenceOwner?.isOuterCandidate(owner) == true &&
            owner.retainedChildSessions.any { it === this })
        if (privateAdditionalResultsAreComplete(owner, system)) {
            removePrivateAdditionalDispatchers(owner)
            return
        }
        val producer = completionCallback as? CfirRetainedAdditionalResultsProducer
        if (producer != null && ownsRegisteredPrivateAdditionalResultsProducer(owner, producer)) return
        privateAdditionalResultsEpoch = Any()
        queuePrivateAdditionalResultsProducer(owner)
    }

    internal fun writePrivateAdditionalResults(
        producer: CfirRetainedAdditionalResultsProducer,
        substitutor: ConeSubstitutor,
        system: ConstraintSystemImpl?,
    ) {
        val owner = producer.owner
        // stale handle 必须在进入 PRIVATE root 前失效；完成事实本身不授予旧 dispatcher 执行权。
        if (!ownsRegisteredPrivateAdditionalResultsProducer(owner, producer)) return
        val writer = checkNotNull(privateAdditionalResultsWriter)
        if (system == null) {
            writer(ownCallbacks.toList(), ownLambdas.toList(), substitutor)
            return
        }
        owner.withConstraintSystemForCompletionResults { check(it === system) }
        check(owner.postponedPCLACalls.none { ownsRegisteredPrivateCallProducer(owner, it) }) {
            "PRIVATE additional results preceded their actual call producers"
        }
        constraintSystemOwner = system
        val revision = privateAdditionalResultsRevision
        val callbacks = ownCallbacks.filter { it !in completedPrivateAdditionalCallbacks }
        val lambdas = ownLambdas.toList()
        writer(callbacks, lambdas, substitutor)
        if (privateAdditionalResultsEpoch !== producer.epoch || privateAdditionalResultsOwner !== owner ||
            privateAdditionalResultsWriter !== writer || retired || registry[resource] !== this) return
        completedPrivateAdditionalCallbacks.addAll(callbacks)
        if (revision !== privateAdditionalResultsRevision ||
            owner.postponedPCLACalls.any { ownsRegisteredPrivateCallProducer(owner, it) }) {
            privateAdditionalResultsCompletion = null
            queuePrivateAdditionalResultsProducer(owner)
            return
        }
        check(privateOriginDepth == 0 && constraintSystemOwner === system && completionOwnerCandidate === owner)
        privateAdditionalResultsCompletion = PrivateAdditionalResultsCompletion(
            writer, owner, system, system.captureStorage(), revision, ownCallbacks.toList(), lambdas,
        )
        removePrivateAdditionalDispatchers(owner)
    }

    private fun invalidatePrivateAdditionalResults() {
        privateAdditionalResultsRevision = Any()
        privateAdditionalResultsCompletion = null
    }

    /** 与 Source/root/AST/CS 同边界捕获事实与注册顺序；restore 总是创建新的 dispatcher epoch。 */
    private fun capturePrivateAdditionalResultsState(): (Candidate?) -> Unit {
        val writer = privateAdditionalResultsWriter
        val owner = privateAdditionalResultsOwner
        val callback = completionCallback
        val fact = privateAdditionalResultsCompletion
        val revision = privateAdditionalResultsRevision
        val completedCallbacks = completedPrivateAdditionalCallbacks.toList()
        val queue = owner?.onPCLACompletionResultsWritingCallbacks?.toList().orEmpty()
        val index = queue.indexOfFirst { it === callback }
        return { reinferenceOwner ->
            listOfNotNull(privateAdditionalResultsOwner, owner, completionOwnerCandidate).distinct()
                .forEach(::removePrivateAdditionalDispatchers)
            val restoredOwner = if (writer == null) owner else reinferenceOwner ?: checkNotNull(owner)
            privateAdditionalResultsWriter = writer
            privateAdditionalResultsOwner = restoredOwner
            privateAdditionalResultsEpoch = Any()
            privateAdditionalResultsRevision = revision
            privateAdditionalResultsCompletion = fact?.takeIf { it.owner === restoredOwner }
            completedPrivateAdditionalCallbacks.clear()
            if (restoredOwner === owner) completedPrivateAdditionalCallbacks.addAll(completedCallbacks)
            if (writer == null) {
                completionCallback = callback
            } else {
                val actualOwner = checkNotNull(restoredOwner)
                val restored = CfirRetainedAdditionalResultsProducer(this, actualOwner, privateAdditionalResultsEpoch)
                completionCallback = restored
                if (index >= 0) {
                    val actual = actualOwner.onPCLACompletionResultsWritingCallbacks
                    val restoredIndex = if (actualOwner !== owner) actual.size else {
                        val following = queue.drop(index + 1).firstNotNullOfOrNull { item ->
                            actual.indexOfFirst { it === item }.takeIf { it >= 0 }
                        }
                        following ?: queue.take(index).asReversed().firstNotNullOfOrNull { item ->
                            actual.indexOfFirst { it === item }.takeIf { it >= 0 }?.plus(1)
                        } ?: 0
                    }
                    actual.add(restoredIndex, restored)
                }
            }
        }
    }

    /** Source 发布验证真实执行事实，不接受 no-types、空队列或 provisional callback 作为证明。 */
    internal fun validatePrivateAdditionalResultsCompletion(system: ConstraintSystemImpl? = null) {
        if (privateAdditionalResultsWriter == null) {
            check(ownCallbacks.isEmpty() && ownLambdas.isEmpty()) {
                "PRIVATE additional results have no registered producer"
            }
            return
        }
        val owner = checkNotNull(privateAdditionalResultsOwner)
        val actual = system ?: checkNotNull(constraintSystemOwner)
        owner.withConstraintSystemForCompletionResults { check(it === actual) }
        check(completionOwnerCandidate === owner && constraintSystemOwner === actual &&
            privateAdditionalResultsAreComplete(owner, actual) &&
            owner.onPCLACompletionResultsWritingCallbacks.none {
                it is CfirRetainedAdditionalResultsProducer && it.graph === this
            }) { "Resource source preceded actual PRIVATE additional results completion" }
    }


    /**
     * retained 调用的生产阶段由真实注册 owner、PRIVATE scope、carrier 和 writer 身份决定。
     * 普通 callbacks 与 Source 槽不参与分类；FORMAL carrier 仍由正式树 writer 消费。
     */
    internal fun ownsRegisteredPrivateCallProducer(owner: Candidate, call: ConeResolutionAtom): Boolean {
        val candidate = (call as? org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate)?.candidate ?: return false
        val scope = candidate.implicitTypeComputationScope ?: return false
        if (completionOwnerCandidate !== owner || scope.graph !== this ||
            scope.mode != CfirImplicitTypeComputationMode.PRIVATE || candidate in withdrawnCandidates ||
            !ownsPrivateElement(call.expression) || ownCalls.none { it === call }) return false
        check(writeCall != null && owner.pclaCallWriters[call] === writeCall) {
            "Registered PRIVATE producer lost its actual writer binding"
        }
        return true
    }

    /** 实际 PRIVATE writer 返回后消费其排队任务；这里从不制造 FORMAL replay。 */
    internal fun completeRetainedPrivateCallProducer(
        owner: Candidate,
        call: ConeResolutionAtom,
        system: ConstraintSystemImpl,
        substitutor: ConeSubstitutor,
    ) {
        check(!retired && registry[resource] === this && system === owner.system)
        check(ownsRegisteredPrivateCallProducer(owner, call))
        val candidate = (call as org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate).candidate
        val scope = checkNotNull(candidate.implicitTypeComputationScope)
        val key = scope.key(candidate)
        val beforeTerminal = terminals[key.callSlot()]
        val registeredWriter = checkNotNull(writeCall)
        registeredWriter(call, substitutor)
        check(!retired && registry[resource] === this && completionOwnerCandidate === owner)
        check(owner.pclaCallWriters[call] === registeredWriter) { "PRIVATE producer changed its registered writer" }
        val instance = checkNotNull(privateInstantiations[candidate])
        if (instance.outcome == CfirRetainedCallOutcome.REJECTED) {
            val reference = (call.expression as? CfirResolvable)?.calleeReference
            check(!candidate.isSuccessful || reference is CfirDiagnosticHolder ||
                reference is CfirNamedReferenceWithCandidate && reference.isError ||
                call.expression.coneTypeOrNull?.contains { it is ConeErrorType } == true)
        } else {
            val terminal = checkNotNull(terminals[key.callSlot()])
            check(instance.outcome == CfirRetainedCallOutcome.COMPLETED &&
                terminal !== beforeTerminal && terminal.sourceGeneration === generation &&
                terminal.instantiation === instance && key == instance.key) {
                "PRIVATE producer returned without publishing its actual final terminal"
            }
        }
        checkNotNull(inferenceOwner).consumeRetainedCallProducer(owner, call)
    }

    /** private analyzer 的新任务仍由其 graph/DFA 写回；constraints 始终留在传入的 actual owner CS。 */
    @OptIn(org.cangnova.cangjie.resolve.calls.inference.model.UnstableSystemMergeMode::class)
    fun capturePrivateAnalysisResults(
        candidate: Candidate,
        before: CandidatePCLACompletionState,
        previousVariables: Set<TypeConstructorMarker>,
        previousSelectedCandidates: Set<Candidate>,
    ) {
        completionOwnerCandidate = candidate
        constraintSystemOwner = candidate.system
        // 晚解析产生的独立 FULL 森林根也移交实际 owner；不导入内层过期的 partial 快照。
        for (selected in privateSelectedCandidates) {
            if (selected in previousSelectedCandidates) continue
            val storage = checkNotNull(privateInstantiations[selected]?.storage)
            if (!storage.usesOuterCs && storage.notFixedTypeVariables.isEmpty()) candidate.system.mergeOtherSystem(storage)
        }
        val storage = candidate.system.captureStorage()
        val dependencies = candidate.retainedChildSessions.filter { it !== this }
        val dependencyVariables = dependencies.flatMapTo(hashSetOf()) { it.ownVariableConstructors }
        // Stage 2 可在 analyzer 回调前通过本图真实槽分配变量；不能只取回调内 CS 的差集。
        val registeredVariables = privateInstantiations.values.flatMap { instantiation ->
            instantiation.variables.orEmpty() + instantiation.associatedVariables.values
        } + expressionVariables.values
        val registeredConstructors = registeredVariables.mapTo(hashSetOf()) { it.typeConstructor }
        val additionalVariables = ((storage.allTypeVariables.keys - previousVariables) +
                (registeredConstructors intersect storage.allTypeVariables.keys)) - incomingVariables - dependencyVariables
        ownVariableConstructors = ownVariableConstructors + additionalVariables
        retainOrigins(storage, additionalVariables)
        candidate.additionalCompletionVariables += ownVariableConstructors
        val previousCalls = before.calls.toSet()
        val calls = candidate.postponedPCLACalls.filter { it !in previousCalls && ownsPrivateElement(it.expression) }
        ownCalls = ownCalls + calls
        for (call in calls) candidate.pclaCallWriters[call] = checkNotNull(writeCall)
        val previousLambdas = before.lambdas.toSet()
        val lambdas = candidate.lambdasAnalyzedWithPCLA.filter { it !in previousLambdas && ownsPrivateElement(it) }
        ownLambdas = ownLambdas + lambdas
        candidate.lambdasAnalyzedWithPCLA.removeAll { it in lambdas }
        val childCallbacks = candidate.retainedChildSessions.filter { it !== this }.mapNotNull { it.completionCallback }.toSet()
        val previousCallbacks = before.callbacks.toSet()
        val callbacks = candidate.onPCLACompletionResultsWritingCallbacks.filter {
            it !in previousCallbacks && it !in childCallbacks && it !== completionCallback &&
                it !is CfirResourceSourceCompletion && it !is CfirRetainedAdditionalResultsProducer
        }
        ownCallbacks = ownCallbacks + callbacks
        candidate.onPCLACompletionResultsWritingCallbacks.removeAll { it in callbacks }
        rebindPrivateAdditionalResultsProducerOwner(candidate)
        if (calls.isNotEmpty() || lambdas.isNotEmpty() || callbacks.isNotEmpty()) invalidatePrivateAdditionalResults()
        completionCallback?.let { callback ->
            if (callback !in candidate.onPCLACompletionResultsWritingCallbacks) candidate.onPCLACompletionResultsWritingCallbacks += callback
        }
    }

    private interface OriginBinding {
        fun constructor(): TypeConstructorMarker
        fun current(): Any
        fun privateOrigin(): Any
        fun sourceOrigin(): Any
        fun relocate(target: Any)
        fun projectedSource(scope: CfirImplicitTypeComputationScope?): Any
        fun adopt(scope: CfirImplicitTypeComputationScope): OriginBinding
    }
    private class ParameterOriginBinding(
        val variable: ConeTypeParameterBasedTypeVariable,
        val clone: CfirTypeParameterSymbol,
        val source: CfirTypeParameterSymbol,
    ) : OriginBinding {
        override fun constructor(): TypeConstructorMarker = variable.typeConstructor
        override fun current(): Any = variable.typeParameterSymbol
        override fun privateOrigin(): Any = clone
        override fun sourceOrigin(): Any = source
        override fun projectedSource(scope: CfirImplicitTypeComputationScope?): Any =
            scope?.projectSymbolFromCanonical(source) ?: source
        override fun adopt(scope: CfirImplicitTypeComputationScope): OriginBinding =
            ParameterOriginBinding(variable, scope.projectSymbolFromCanonical(source) as CfirTypeParameterSymbol, source)
        override fun relocate(target: Any) {
            val symbol = target as CfirTypeParameterSymbol
            if (variable.typeParameterSymbol !== symbol) variable.relocateTypeParameterOrigin(variable.typeParameterSymbol, symbol)
        }
    }
    private class LambdaOriginBinding(
        val variable: ConeTypeVariableForLambdaReturnType,
        val clone: CfirDeclaration,
        val source: CfirDeclaration,
    ) : OriginBinding {
        override fun constructor(): TypeConstructorMarker = variable.typeConstructor
        override fun current(): Any = variable.argument
        override fun privateOrigin(): Any = clone
        override fun sourceOrigin(): Any = source
        override fun projectedSource(scope: CfirImplicitTypeComputationScope?): Any =
            scope?.projectElementFromCanonical(source) ?: source
        override fun adopt(scope: CfirImplicitTypeComputationScope): OriginBinding =
            LambdaOriginBinding(variable, scope.projectElementFromCanonical(source) as CfirDeclaration, source)
        override fun relocate(target: Any) {
            val declaration = target as CfirDeclaration
            if (variable.argument !== declaration) variable.relocateArgument(variable.argument, declaration)
        }
    }
    private val origins = mutableListOf<OriginBinding>()
    private var sourceProjectionScope: CfirImplicitTypeComputationScope? = null
    private var hasSourceProjection = false
    private var privateOriginDepth = 0
    internal var ownVariableConstructors: Set<TypeConstructorMarker> = emptySet()
        private set

    /** 只登记本次新增变量；嵌套图和 active parent 的变量继续由各自 owner 维护。 */
    fun retainVariableOrigins(storage: ConstraintStorage, dependencies: List<CfirImplicitTypeComputation>) {
        val dependencyVariables = dependencies.flatMapTo(hashSetOf()) { it.ownVariableConstructors }
        ownVariableConstructors = storage.allTypeVariables.keys - incomingVariables - dependencyVariables
        retainOrigins(storage, ownVariableConstructors)
    }

    /** 初始移交与晚任务分配使用同一 origin 登记算法，不改动 incoming/dependency 的 origin。 */
    private fun retainOrigins(storage: ConstraintStorage, constructors: Set<TypeConstructorMarker>) {
        for (constructor in constructors) {
            if (origins.any { it.constructor() === constructor }) continue
            when (val variable = storage.allTypeVariables.getValue(constructor)) {
                is ConeTypeParameterBasedTypeVariable -> {
                    val clone = variable.typeParameterSymbol
                    val source = canonicalSymbol(clone) as CfirTypeParameterSymbol
                    if (source !== clone) origins += ParameterOriginBinding(variable, clone, source)
                }
                is ConeTypeVariableForLambdaReturnType -> {
                    val clone = variable.argument
                    val source = canonicalPrivateElement(clone) as CfirDeclaration
                    origins += LambdaOriginBinding(variable, clone, source)
                }
                is org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType,
                is org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForPostponedAtom -> Unit
                else -> check(variable.javaClass == ConeTypeVariable::class.java) {
                    "A retained inference variable has no origin policy: ${variable.javaClass.name}"
                }
            }
        }
    }

    /** 正式图取得源声明 origin；constructor 与 inference scope 的身份均保持不变。 */
    fun projectOriginsToSource(targetScope: CfirImplicitTypeComputationScope?) {
        sourceProjectionScope = targetScope
        hasSourceProjection = true
        origins.forEach { it.relocate(it.projectedSource(targetScope)) }
    }

    /** 私有父图中的正式 replay 仍属于该父树；origin authority 移交后只保留一份控制者。 */
    private fun adoptOriginsFrom(child: CfirImplicitTypeComputation, scope: CfirImplicitTypeComputationScope) {
        for (binding in child.origins) {
            val previous = origins.firstOrNull { it.constructor() === binding.constructor() }
            check(previous == null || previous.sourceOrigin() === binding.sourceOrigin()) {
                "One retained constructor acquired two declaration origins"
            }
            if (previous == null) origins += binding.adopt(scope)
        }
        ownVariableConstructors = ownVariableConstructors + child.ownVariableConstructors
        child.origins.clear()
    }

    /** 私有晚任务显式使用本图 origin，退出后恢复当时的正式/私有视图。 */
    fun <T> withPrivateOrigins(block: () -> T): T {
        check(!retired && registry[resource] === this) { "Private analyzer belongs to a retired or unregistered resource graph" }
        val previous = IdentityHashMap<OriginBinding, Any>().apply {
            origins.forEach { put(it, it.current()) }
        }
        privateOriginDepth++
        return try {
            origins.forEach { it.relocate(it.privateOrigin()) }
            block()
        } finally {
            privateOriginDepth--
            for (binding in origins) {
                // 晚 analyzer 和 nested replay 都能新增/adopt binding；新 binding 也必须退出私有视图。
                val target = previous[binding] ?: when {
                    privateOriginDepth > 0 -> binding.privateOrigin()
                    hasSourceProjection -> binding.projectedSource(sourceProjectionScope)
                    else -> binding.privateOrigin()
                }
                binding.relocate(target)
            }
        }
    }

    fun storeResult(result: CfirResolvedTypeRef) {
        check(resultTypeRef == null)
        resultTypeRef = result
    }

    fun retainResults(
        calls: List<ConeResolutionAtom>,
        callbacks: List<(ConeSubstitutor) -> Unit>,
        lambdas: List<CfirDeclaration>,
        projectionOwners: List<CfirElement>,
    ) {
        ownCalls = calls
        ownCallbacks = callbacks
        ownLambdas = lambdas
        ownProjectionOwners = projectionOwners
        invalidatePrivateAdditionalResults()
    }

    private val formalResourceSources = mutableListOf<CfirResourceSourceCompletion>()
    internal var privateInitializerContext: org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext? = null
        private set
    internal var privateInitializerOwner: CfirResourceVariable? = null
        private set

    /** 只从真实 computeResourceType 入口登记 PRIVATE 根，不按名字/offset/context scope 猜 owner。 */
    internal fun registerPrivateInitializerContext(
        owner: CfirResourceVariable,
        context: org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext,
    ) {
        check(privateInitializerContext == null && privateInitializerOwner == null)
        val root = context.dataFlowAnalyzerContext.graphBuilder.currentGraph
        check(root.kind == org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph.Kind.ResourceInitializer &&
            root.declaration === owner)
        check(context.implicitTypeComputationScope?.let {
            it.graph === this && it.mode == CfirImplicitTypeComputationMode.PRIVATE
        } == true) { "Resource initializer root belongs to another inference graph" }
        privateInitializerContext = context
        privateInitializerOwner = owner
    }

    /** Source 的真实 final 检查通过后关闭 PRIVATE CFG；普通 callback 返回不授予完成资格。 */
    internal fun finishPrivateInitializerRoot() {
        val context = privateInitializerContext
        if (context == null) {
            check(privateInitializerOwner == null)
            return
        }
        val owner = checkNotNull(privateInitializerOwner)
        context.dataFlowAnalyzerContext.graphBuilder.finishResourceInitializer(owner)
    }

    internal fun bindFormalResourceSource(task: CfirResourceSourceCompletion) {
        check(!retired && registry[resource] === this)
        check(formalResourceSources.none { it.isCurrent && it.resource === task.resource })
        formalResourceSources += task
    }

    private fun cancelResourceSourceCallbacks() {
        formalResourceSources.forEach { it.cancel() }
        privateAdditionalResultsOwner?.let(::removePrivateAdditionalDispatchers)
        privateAdditionalResultsEpoch = Any()
        privateAdditionalResultsCompletion = null
    }

    internal fun validateResourceSourceOwner(task: CfirResourceSourceCompletion) {
        check(!retired && registry[resource] === this && formalResourceSources.any { it === task && it.isCurrent }) {
            "Resource source completion changed its actual graph owner"
        }
    }

    internal fun publishFormalResourceType(task: CfirResourceSourceCompletion, type: CfirResolvedTypeRef) {
        validateResourceSourceOwner(task)
        check(task.isCompleted)
        resultTypeRef = type
    }

    internal fun validatePrivateResultsRetirement(sourceRejected: Boolean) {
        check(privateOriginDepth == 0) { "Resource source published while a PRIVATE analyzer or writer was still active" }
        validatePrivateAdditionalResultsCompletion()
        completionOwnerCandidate?.let { owner ->
            check(owner.postponedPCLACalls.none { ownsRegisteredPrivateCallProducer(owner, it) }) {
                "Resource source published before its registered PRIVATE call producers completed"
            }
        }
        // formal partial 也有完成义务，不能只检查 PRIVATE completed terminal 的非空表。
        formalSelectedAllocations.forEach { (candidate, instance) ->
            val site = instance.key.callSlot()
            // 声明错误不能把仍待实际 writer 完成的调用升级为已拒绝；PARTIAL 始终阻止退休。
            check(formalOutcomes[site] != null && formalOutcomes[site] != CfirRetainedCallOutcome.PARTIAL &&
                (sourceRejected || formalOutcomes[site] == CfirRetainedCallOutcome.COMPLETED ||
                    formalOutcomes[site] == CfirRetainedCallOutcome.REJECTED || site.site in formalSourceRejections)) {
                "Resource source retired before its formal call received a final outcome: " +
                    "resource=${resource.name}@${System.identityHashCode(resource)}, " +
                    "site=${site.site::class.simpleName}@${System.identityHashCode(site.site)}, " +
                    "call=${candidate.callInfo.name}@${System.identityHashCode(candidate)}, outcome=${formalOutcomes[site]}"
            }
        }
        validateRetainedResults(sourceRejected)
    }

    /** 正式遍历只退役本图的源码写回任务；已移交的约束与变量保持在父系统。 */
    fun retirePrivateResults(targetScope: CfirImplicitTypeComputationScope?, sourceRejected: Boolean) {
        check(formalResourceSources.none { it.isCurrent && !it.isCompleted }) { "Resource source is still awaiting its actual final writer" }
        validatePrivateResultsRetirement(sourceRejected)
        // 所有真实选中实例已通过完成验证；其余仅分配的探索候选撤销，不制造 REJECTED/COMPLETED。
        formalAllocations.keys.toList().filter { it !in formalSelectedAllocations }.forEach { candidate ->
            formalAllocations.remove(candidate)
            withdrawnCandidates += candidate
        }
        retired = true
        completionLeases.clear()
        formalWriterBindings.clear()
        inferenceOwner?.retireChildSession(this)
        inferenceOwner = null
        writeCall = null
        completionCallback = null
        analyzePostponedAtom = null
        targetScope?.privateTreeScope()?.let { scope -> scope.graph.adoptOriginsFrom(this, scope) }
    }

    private fun validateRetainedResults(sourceRejected: Boolean) {
        for ((site, terminal) in terminals) {
            when (formalOutcomes[site]) {
                CfirRetainedCallOutcome.COMPLETED -> check(
                    terminal.instantiation in replayedInstantiations &&
                        completedFormalBindings[site]?.binding?.let { binding ->
                            binding.terminal === terminal && binding.generation === terminal.generation &&
                                binding.instantiation === terminal.instantiation
                        } == true
                ) {
                    // 只输出真实 terminal、选择实例与 VM 身份，保留原 replay 完成义务。
                    fun identity(value: Any?): String = if (value == null) "null" else System.identityHashCode(value).toString(16)
                    fun describe(instance: Instantiation): String =
                        "instance=${identity(instance)},site=${identity(instance.key.callSite)},target=${instance.key.target}," +
                                "kind=${instance.key.kind},origin=${instance.key.origin},invoke=${instance.key.invokeMode}," +
                                "provenance=${instance.key.provenance},outcome=${instance.outcome}," +
                                "variables=${instance.variables?.map { identity(it) }}," +
                                "associated=${instance.associatedVariables.values.map { identity(it) }}"
                    "Completed retained terminal has no actual formal replay: " +
                            "graph=${identity(this)},resource=${identity(resource)},site=${identity(site.site)}," +
                            "currentGeneration=${identity(generation)},terminalGeneration=${identity(terminal.generation)}," +
                            "terminalSourceGeneration=${identity(terminal.sourceGeneration)}," +
                            "formalBindingGeneration=${identity(completedFormalBindings[site]?.binding?.generation)}," +
                            "terminal={${describe(terminal.instantiation)}}," +
                            "selected=${formalSelectedAllocations.filterValues { it.key.callSlot() == site }.map { (candidate, instance) -> "candidate=${identity(candidate)}:{${describe(instance)}}" }}," +
                            "replayed=${replayedInstantiations.map(::describe)}"
                }
                CfirRetainedCallOutcome.REJECTED -> Unit // 显式源码拒绝，与成功消费分别保留。
                else -> check(sourceRejected || site.site in formalSourceRejections) { "Retained terminal has no final source outcome" }
            }
        }
        for ((slot, use) in privateExpressionUses) {
            if (!use.accepted) continue
            val formal = formalExpressionUses[slot]
            if (formal != null) check(formal.generation === use.generation) { "Formal consumption used a revoked expression generation" }
            check(formal != null || sourceRejected) { "Retained expression slot has no real formal consumer" }
        }
    }
}

/** 真实 writer 的源码域与实际 CS 凭证；不使用时间戳或全图可变 epoch。 */
internal class CfirRetainedCompletionLease internal constructor(
    internal val graph: CfirImplicitTypeComputation,
    internal val sources: Set<CfirElement>,
    internal val system: ConstraintSystemImpl,
    internal val mode: CfirImplicitTypeComputationMode,
) {
    internal val grantedSources: MutableSet<CfirElement> = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>()).apply { addAll(sources) }
}

/** 只与产生它的 AST/cache/CS 边界一起恢复；reinfer 不撤销已经求出的真实绑定。 */
internal class CfirImplicitTypeReplaySnapshot(private val restore: (ConstraintSystemImpl?) -> Unit) {
    fun restoreForRollback() = restore(null)
    fun restoreForReinference(system: ConstraintSystemImpl) = restore(system)
}

/** 每个角色对应一个实际语义槽；index 只表示参数或分量位置。 */
internal enum class CfirInferenceVariableRole {
    LAMBDA_PARAMETER, LAMBDA_RETURN, FACTORY_LAMBDA_RETURN,
    INVOKE_PARAMETER, INVOKE_RETURN, OPTION_PATTERN_ELEMENT, ITERABLE_ELEMENT,
    COMMON_LAMBDA_RETURN, COMMON_LAMBDA_PARAMETER, CALLABLE_REFERENCE_RETURN, CALLABLE_REFERENCE_PARAMETER,
}

/** completed 只由最终 writer 建立；源码拒绝不冒充成功 replay。 */
internal enum class CfirRetainedCallOutcome { SKIPPED, PARTIAL, REJECTED, COMPLETED }

internal enum class CfirImplicitTypeComputationMode { PRIVATE, FORMAL }

/** 由真正生成该调用的代码路径登记，不能由名字或 AST 类推测。 */
internal enum class CfirSyntheticCallRole { LAMBDA_ACCEPT, FUNCTION_TYPE_INVOKE, IF_SELECT, MATCH_SELECT, TRY_SELECT }

/** 当前转换对图的访问模式；实例按词法调用栈进入与退出，不存放在全局 DEFAULT 会话。 */
internal class CfirImplicitTypeComputationScope(
    val graph: CfirImplicitTypeComputation,
    val mode: CfirImplicitTypeComputationMode,
    private val targetScope: CfirImplicitTypeComputationScope? = null,
) {
    fun canonicalElement(element: CfirElement): CfirElement = graph.derivedSite(element) ?: when (mode) {
        CfirImplicitTypeComputationMode.PRIVATE -> graph.canonicalPrivateElement(element)
        CfirImplicitTypeComputationMode.FORMAL -> targetScope?.canonicalElement(element) ?: element
    }
    /** 只有真正分配过表达式槽的 owner 才属于消费域；借用 receiver 不制造槽。 */
    fun canonicalExpressionSlotOwner(element: CfirElement): CfirElement? = graph.derivedSite(element) ?: when (mode) {
        CfirImplicitTypeComputationMode.PRIVATE -> graph.canonicalOwnedPrivateElementOrNull(element)
        CfirImplicitTypeComputationMode.FORMAL -> if (targetScope == null) element else targetScope.canonicalExpressionSlotOwner(element)
    }
    fun registerDerivedElement(source: CfirElement, derived: CfirElement) = graph.registerDerivedElement(this, source, derived)
    fun canonicalSymbol(symbol: CfirBasedSymbol<*>): CfirBasedSymbol<*> = graph.canonicalSymbol(symbol)
    fun projectElementFromCanonical(element: CfirElement): CfirElement = when (mode) {
        CfirImplicitTypeComputationMode.PRIVATE -> graph.privateElementForCanonical(element)
        CfirImplicitTypeComputationMode.FORMAL -> targetScope?.projectElementFromCanonical(element) ?: element
    }
    fun projectSymbolFromCanonical(symbol: CfirBasedSymbol<*>): CfirBasedSymbol<*> = when (mode) {
        CfirImplicitTypeComputationMode.PRIVATE -> graph.privateSymbolForCanonical(symbol)
        CfirImplicitTypeComputationMode.FORMAL -> targetScope?.projectSymbolFromCanonical(symbol) ?: symbol
    }
    fun privateTreeScope(): CfirImplicitTypeComputationScope? = when (mode) {
        CfirImplicitTypeComputationMode.PRIVATE -> this
        CfirImplicitTypeComputationMode.FORMAL -> targetScope?.privateTreeScope()
    }
}
