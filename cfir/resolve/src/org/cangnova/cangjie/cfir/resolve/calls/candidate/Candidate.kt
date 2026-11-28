/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.calls.candidate

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.cfir.resolve.CfirSamResolver
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.CallableReferenceAdaptation
import org.cangnova.cangjie.cfir.resolve.calls.ArgumentMappingOutcome
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceVariableRole
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.semantics.hasBareGenericFunctionReferencePayloadArgument
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.resolve.recoverableFunctionTypeOrNull
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 记录一次 fresh lambda receiver 成员访问中需要从 PCLA common system 移除的 receiver 约束。
 */
internal data class FreshReceiverConstraintToDrop(
    /** fresh lambda 参数对应的类型变量构造器。 */
    val receiverTypeConstructor: TypeConstructorMarker,
    /** 当前成员访问的 receiver 表达式。 */
    val receiverExpression: CfirExpression,
)

/**
 * all-candidates 路径的结构候选身份。
 *
 * 同一个最终 symbol 可以由不同 extend/interface 父边到达；这些来源具有不同访问面和
 * 诊断含义，不能再按 symbol 单独去重。
 */
internal data class CfirCandidateLookupIdentity(
    val symbol: CfirBasedSymbol<*>,
    val lookupProvenance: CfirCallableLookupProvenance,
)

/**
 * 正式 FULL factory 为一个实际 PCLA 队列 owner 创建的写入代次。
 * writer epoch 不进入 memento；immutable fact 只依赖快照可恢复的 analysis generation。
 */
internal class CfirUnscopedCompletionPass(
    val owner: Candidate,
    val producer: org.cangnova.cangjie.cfir.resolve.inference.CfirFullCompletionAuthority,
    val ownerSystem: ConstraintSystemImpl,
) {
    val system: ConstraintSystemImpl get() = producer.system
    val ownerGeneration: Any = owner.unscopedCompletionGeneration
    val writerEpoch: Any = owner.unscopedWriterEpoch

    /** 历史 producer 的完成事实不携带可复活的 writer 权限。 */
    fun matchesProducerFact(): Boolean = !producer.candidate.isSourceCompletionWithdrawn &&
        producer.candidate.withConstraintSystemForCompletionResults { actualSystem ->
        producer.system === actualSystem &&
            producer.generation === producer.candidate.unscopedCompletionGeneration &&
            producer.candidate.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(producer.candidate) != true
    }
}

/** 在修改 queued carrier 之前冻结实际 atom、candidate、target 与 VM；不能从 resolved/type 推导入口。 */
internal class CfirUnscopedCompletionEntry(val pass: CfirUnscopedCompletionPass, val atom: ConeAtomWithCandidate) {
    val candidate: Candidate = atom.candidate
    val carrier: CfirExpression = atom.expression
    val callSite: CfirElement = candidate.callInfo.callSite
    val target: CfirBasedSymbol<*> = candidate.symbol
    val candidateSystem: ConstraintSystemImpl = candidate.withConstraintSystemForCompletionResults { it }
    private val candidateGeneration = candidate.unscopedCompletionGeneration
    private val substitutor = candidate.substitutor
    val freshVariables = candidate.freshVariables.toList()
    val additionalVariables = candidate.additionalCompletionVariables.toList()

    fun matchesCandidate(): Boolean =
        !candidate.isSourceCompletionWithdrawn && candidateSystem.withParentRebaseSuspended {
            atom.candidate === candidate && atom.expression === carrier && candidate.implicitTypeComputationScope == null &&
                candidate.callInfo.callSite === callSite && candidate.symbol === target && candidate.system === candidateSystem &&
                candidate.unscopedCompletionGeneration === candidateGeneration && candidate.substitutor === substitutor &&
                candidate.freshVariables.size == freshVariables.size &&
                candidate.freshVariables.zip(freshVariables).all { (a, b) -> a === b } &&
                candidate.additionalCompletionVariables.size == additionalVariables.size &&
                candidate.additionalCompletionVariables.all { current -> additionalVariables.any { it === current } }
        }
}

/** 固定解来自真正的 completion CS；保存类型快照，不保存会随系统修改的 map view。 */
internal class CfirUnscopedFixedBinding(val variable: ConeTypeVariable, val type: ConeCangJieType)

/**
 * 一个实际 queued atom 的最终写回证据；只由生产 FULL writer 在成功返回点创建。
 * 它不能证明另一 candidate、atom、carrier、queue owner 或 CS 已完成。
 */
internal class CfirUnscopedCompletionReceipt(
    val entry: CfirUnscopedCompletionEntry,
    val reference: CfirResolvedNamedReference,
    val type: ConeCangJieType,
    private val bindings: List<CfirUnscopedFixedBinding>,
) {
    private val argumentList = (entry.carrier as? CfirFunctionCall)?.argumentList
    private val arguments = argumentList?.arguments?.toList().orEmpty()
    private val argumentTypes = arguments.map { it.coneTypeOrNull }
    private val receiver = (entry.carrier as? CfirQualifiedAccessExpression)?.explicitReceiver
    private val receiverType = receiver?.coneTypeOrNull
    private val dispatchReceiver = (entry.carrier as? CfirQualifiedAccessExpression)?.dispatchReceiver
    private val dispatchReceiverType = dispatchReceiver?.coneTypeOrNull
    private val typeArguments = (entry.carrier as? CfirQualifiedAccessExpression)?.typeArguments?.toList().orEmpty()
    private val typeArgumentTypes = typeArguments.map { it.coneTypeOrNull }
    private val candidateArguments = entry.candidate.arguments.toList()
    private val mapping = entry.candidate.argumentMapping.entries.map { it.key to it.value }

    /** 完成事实只能消耗它自己的 atom，不能遮住同 carrier 的另一未选择义务。 */
    fun canConsume(owner: Candidate, system: ConstraintSystemImpl): Boolean {
        if (!matchesCurrentResult()) return owner.hasActualUnscopedReplacement(entry.carrier, entry.candidate, system)
        return owner.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>().none { other ->
            other !== entry.atom && other.expression === entry.carrier && owner.pclaCallWriters[other] == null &&
                owner.unscopedCompletionReceipt(other, system) == null
        }
    }

    fun matchesCurrentResult(): Boolean {
        val call = entry.carrier as? CfirFunctionCall
        val currentArguments = call?.argumentList?.arguments.orEmpty()
        val qualified = entry.carrier as? CfirQualifiedAccessExpression
        return (entry.carrier as? CfirResolvable)?.calleeReference === reference && entry.carrier.coneTypeOrNull === type &&
            call?.argumentList === argumentList && currentArguments.size == arguments.size &&
            currentArguments.zip(arguments).all { (a, b) -> a === b } &&
            currentArguments.map { it.coneTypeOrNull }.zip(argumentTypes).all { (a, b) -> a === b } &&
            qualified?.explicitReceiver === receiver && receiver?.coneTypeOrNull === receiverType &&
            qualified?.dispatchReceiver === dispatchReceiver && dispatchReceiver?.coneTypeOrNull === dispatchReceiverType &&
            qualified?.typeArguments.orEmpty().size == typeArguments.size &&
            qualified?.typeArguments.orEmpty().zip(typeArguments).all { (a, b) -> a === b } &&
            qualified?.typeArguments.orEmpty().map { it.coneTypeOrNull }.zip(typeArgumentTypes).all { (a, b) -> a === b }
    }

    fun isValidFor(owner: Candidate, atom: ConeAtomWithCandidate, system: ConstraintSystemImpl): Boolean =
        entry.candidateSystem.withParentRebaseSuspended {
            if (owner.isSourceCompletionWithdrawn || entry.pass.owner !== owner || entry.atom !== atom || entry.pass.system !== system ||
                entry.pass.ownerSystem !== owner.withConstraintSystemForCompletionResults { it } || entry.pass.ownerGeneration !== owner.unscopedCompletionGeneration || !entry.pass.matchesProducerFact() ||
                owner.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(owner) == true || !entry.matchesCandidate() ||
                reference.resolvedSymbol !== entry.target || !entry.candidate.isSuccessful ||
                entry.candidate.arguments.size != candidateArguments.size ||
                !entry.candidate.arguments.zip(candidateArguments).all { (a, b) -> a === b } ||
                entry.candidate.argumentMapping.size != mapping.size ||
                !entry.candidate.argumentMapping.entries.zip(mapping).all { (actual, saved) -> actual.key === saved.first && actual.value === saved.second } ||
                entry.candidate.postponedAtoms.any { !it.analyzed }
            ) return@withParentRebaseSuspended false
            val storage = system.captureStorage()
            if (storage.hasContradiction || storage.notFixedTypeVariables.isNotEmpty() || storage.constraintsFromAllForkPoints.isNotEmpty()) return@withParentRebaseSuspended false
            val substitutor = storage.buildCurrentSubstitutor(owner.callInfo.session.typeContext, emptyMap()).asCone()
            val childStorage = entry.candidateSystem.captureStorage()
            val childSubstitutor = childStorage.buildCurrentSubstitutor(owner.callInfo.session.typeContext, emptyMap()).asCone()
            bindings.all { binding ->
                val variable = binding.variable
                val fixed = storage.fixedTypeVariables[variable.typeConstructor]?.asCone() ?: return@all false
                val solved = substitutor.substituteOrSelf(fixed)
                val childFixed = childStorage.fixedTypeVariables[variable.typeConstructor]?.asCone()
                val childSolved = childFixed?.let { childSubstitutor.substituteOrSelf(it) }
                (childSolved == null || (!childSolved.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType } &&
                    AbstractTypeChecker.equalTypes(owner.callInfo.session.typeContext, childSolved, binding.type))) &&
                storage.allTypeVariables[variable.typeConstructor] === variable &&
                    childStorage.allTypeVariables[variable.typeConstructor] === variable &&
                    !solved.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType } &&
                    AbstractTypeChecker.equalTypes(owner.callInfo.session.typeContext, solved, binding.type)
            }
        }
}

/** PCLA 的写回结果存储；依赖类型试算临时使用副本，调用方持有的原队列对象保持有效。 */
internal class CandidatePCLACompletionState(
    val additionalVariables: MutableSet<TypeConstructorMarker> = linkedSetOf(),
    val calls: MutableList<ConeResolutionAtom> = mutableListOf(),
    val lambdas: MutableList<CfirDeclaration> = mutableListOf(),
    val callbacks: MutableList<(ConeSubstitutor) -> Unit> = mutableListOf(),
    val callWriters: IdentityHashMap<ConeResolutionAtom, (ConeResolutionAtom, ConeSubstitutor) -> Unit> = IdentityHashMap(),
    val childSessions: MutableList<org.cangnova.cangjie.cfir.resolve.inference.CfirImplicitTypeComputation> = mutableListOf(),
    var unscopedAnalysisGeneration: Any = Any(),
    var unscopedPass: CfirUnscopedCompletionPass? = null,
    val unscopedReceipts: IdentityHashMap<ConeAtomWithCandidate, CfirUnscopedCompletionReceipt> = IdentityHashMap(),
    var processedCallsCount: Int = 0,
) {
    /** 复制任务和 immutable 完成事实；writer pass 不进入成功/回滚 memento。 */
    fun copy(): CandidatePCLACompletionState = CandidatePCLACompletionState(
        additionalVariables.toMutableSet(), calls.toMutableList(), lambdas.toMutableList(), callbacks.toMutableList(),
        IdentityHashMap(callWriters), childSessions.toMutableList(),
        unscopedAnalysisGeneration = unscopedAnalysisGeneration,
        unscopedReceipts = IdentityHashMap(unscopedReceipts),
        processedCallsCount = processedCallsCount,
    )
}

/**
 * 单个调用解析候选。
 *
 * Candidate 持有候选符号、receiver、实参映射、约束系统、适用性诊断、变参信息、
 * callable reference / SAM / PCLA 等完成阶段状态，是 tower resolve 到 call completion 的核心状态对象。
 */
class Candidate(
    symbol: CfirBasedSymbol<*>,
    // Here we may have an ExpressionReceiverValue
    // - in case a use-site receiver is explicit
    // - in some cases with static entities, no matter is a use-site receiver explicit or not
    // OR we may have here a kind of ImplicitReceiverValue (non-statics only)
    /** 候选使用的 dispatch receiver atom。 */
    override var dispatchReceiver: ConeResolutionAtom?,
    /** 调用点给出的 extension receiver atom。 */
    val givenExtensionReceiver: ConeResolutionAtom?,
    /** 显式 receiver 的来源种类。 */
    override val explicitReceiverKind: ExplicitReceiverKind,
    /** 创建候选约束系统的工厂。 */
    private val constraintSystemFactory: InferenceComponents.ConstraintSystemFactory,
    /** tower resolve 传入的基础约束系统。 */
    private val baseSystem: ConstraintStorage,
    /** 当前候选所属调用的信息。 */
    override val callInfo: CallInfo,
    /** 产生该候选的 scope。 */
    val originScope: CfirScope?,
    /**
     * tower 名字发现阶段已经计算出的语言级可访问性结果。
     *
     * `null` 表示当前候选由非 tower 的合成入口创建，可见性阶段需要根据显式
     * [CallInfo] 重新调用统一 checker。该字段只缓存本次候选的 use-site 结果，
     * 不进入 scope/session 缓存。
     */
    val discoveryAccessibilityResult: CfirAccessibilityResult? = null,
    /** 候选从 effective member graph 到达 tower 时保留的 extend/interface 来源。 */
    val lookupProvenance: CfirCallableLookupProvenance = CfirCallableLookupProvenance.None,
    /** 候选是否来自 companion object 类型 scope。 */
    val isFromCompanionObjectTypeScope: Boolean = false,
    // It's only true if we're in the member scope of smart cast receiver and this particular candidate came from original type
    /** smart-cast receiver 存在时，候选是否来自原始类型而非 smart-cast 类型。 */
    val isFromOriginalTypeInPresenceOfSmartCast: Boolean = false,
    /** 候选从其实际创建上下文取得当前 outer system，资源图访问域仍单独冻结。 */
    private val bodyResolveContext: BodyResolveContext,
) : AbstractCallCandidate<ConeResolutionAtom>() {

    private var pclaCompletionState = CandidatePCLACompletionState()
    /** writer capability 单调撤销且不进入快照；分析代次和事实由 PCLA memento 恢复。 */
    private var unscopedCompletionEpoch: Any = Any()
    /** 已被成功重查替换的 source producer 永久撤权；与 writer epoch 一样不参与语义 memento。 */
    private var sourceCompletionWithdrawn: Boolean = false

    /** 捕获入口结果，不复制或替换正式候选的约束系统。 */
    internal fun capturePCLACompletionState(): CandidatePCLACompletionState = pclaCompletionState.copy()

    /** AST/CS 事务恢复完整任务与 immutable 完成事实；held writer capability 单独撤销。 */
    internal fun restorePCLACompletionState(state: CandidatePCLACompletionState) {
        revokeUnscopedWriter()
        pclaCompletionState.additionalVariables.clear()
        pclaCompletionState.additionalVariables.addAll(state.additionalVariables)
        pclaCompletionState.calls.clear()
        pclaCompletionState.calls.addAll(state.calls)
        pclaCompletionState.lambdas.clear()
        pclaCompletionState.lambdas.addAll(state.lambdas)
        pclaCompletionState.callbacks.clear()
        pclaCompletionState.callbacks.addAll(state.callbacks)
        pclaCompletionState.callWriters.clear()
        pclaCompletionState.callWriters.putAll(state.callWriters)
        pclaCompletionState.childSessions.clear()
        pclaCompletionState.childSessions.addAll(state.childSessions)
        pclaCompletionState.unscopedAnalysisGeneration = state.unscopedAnalysisGeneration
        pclaCompletionState.unscopedReceipts.clear()
        pclaCompletionState.unscopedReceipts.putAll(state.unscopedReceipts)
        processedPostponedCallsCount = state.processedCallsCount
    }

    /** 每个候选保留创建时的资源图访问域，不能借用后来进入的另一个 initializer。 */
    internal val implicitTypeComputationScope = bodyResolveContext.implicitTypeComputationScope

    internal fun <V : org.cangnova.cangjie.cfir.types.ConeTypeVariable> createInferenceVariable(
        owner: CfirElement,
        role: org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceVariableRole,
        index: Int = 0,
        create: () -> V,
    ): V {
        val scope = implicitTypeComputationScope ?: return create()
        return scope.graph.variable(scope, this, owner, role, index, create)
    }

    // ---------------------------------------- Symbol ----------------------------------------

    /** 当前候选绑定的 CFIR 符号；候选替换或 fake override 规整时可通过受控 API 更新。 */
    override var symbol: CfirBasedSymbol<*> = symbol
        private set

    /**
     * 更新候选符号。
     */
    @UpdatingCandidateInvariants
    fun updateSymbol(symbol: CfirBasedSymbol<*>) {
        this.symbol = symbol
    }

    // ---------------------------------------- Constraint system ----------------------------------------

    /** 候选约束系统是否依赖外层约束系统。 */
    override val usedOuterCs: Boolean get() = system.usesOuterCs

    /** 约束系统是否已经按需初始化。 */
    private var systemInitialized: Boolean = false
    /** 当前候选专属的约束系统，首次访问时根据 base system 和推断 session 初始化。 */
    private val candidateSystem: ConstraintSystemImpl by lazy(LazyThreadSafetyMode.NONE) {
        val system = constraintSystemFactory.createConstraintSystem()

        val baseCSFromInferenceSession = if (baseSystem.usesOuterCs) null else
            bodyResolveContext.inferenceSession.baseConstraintStorageForCandidate(this, bodyResolveContext)
        if (baseSystem.usesOuterCs) {
            system.setBaseSystem(baseSystem)
            system.synchronizeWithOwner(activeConstraintSystemOwner(system))
        } else if (baseCSFromInferenceSession != null) {
            system.setBaseSystem(baseCSFromInferenceSession)
            system.integrateInferenceForest(listOf(baseSystem), requireClosed = false)
        } else {
            system.setBaseSystem(baseSystem)
        }

        implicitTypeComputationScope?.let { scope ->
            if (scope.mode == org.cangnova.cangjie.cfir.resolve.inference.CfirImplicitTypeComputationMode.FORMAL) {
                scope.graph.storageForFormalCandidate(scope, this)?.let { retained ->
                    if (retained.usesOuterCs) {
                        val owner = checkNotNull(scope.graph.constraintSystemOwner)
                        check(owner.allTypeVariables.keys.containsAll(retained.allTypeVariables.keys)) {
                            "Retained shared variables were not transferred to the active parent"
                        }
                        retained.allTypeVariables.forEach { (constructor, variable) ->
                            check(owner.allTypeVariables[constructor] === variable)
                        }
                        val current = owner.buildCurrentSubstitutor().asCone()
                        retained.fixedTypeVariables.forEach { (constructor, type) ->
                            val fixed = checkNotNull(owner.fixedTypeVariables[constructor])
                            check(current.substituteOrSelf(type.asCone()) == current.substituteOrSelf(fixed.asCone())) {
                                "Retained fixed binding disagrees with the active parent"
                            }
                        }
                        // 同一访问在正式正文中可由 proper 类型变为独立调用。它不复制共享前缀，
                        // 但仍复用自己的 constructor，并从真正 owner 读取已完成的绑定。
                        for (variable in scope.graph.variablesForFormalCandidate(scope, this)) {
                            val constructor = variable.typeConstructor
                            val previous = system.allTypeVariables[constructor]
                            if (previous != null) {
                                check(previous === variable)
                                val binding = owner.fixedTypeVariables[constructor]
                                if (binding != null) {
                                    val type = current.substituteOrSelf(binding.asCone())
                                    val existing = system.fixedTypeVariables[constructor]
                                    if (existing == null) {
                                        system.fixVariable(variable, type,
                                            org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition(variable))
                                    } else check(current.substituteOrSelf(existing.asCone()) == type)
                                } else check(constructor !in system.fixedTypeVariables) {
                                    "A shared formal candidate fixed a variable ahead of its actual owner"
                                }
                                continue
                            }
                            val type = current.substituteOrSelf(checkNotNull(owner.fixedTypeVariables[constructor]).asCone())
                            type.forEachTypeIncludingMetadata { component ->
                                check(component !is ConeStubType && (component !is ConeTypeVariableType ||
                                    component.typeConstructor !in owner.notFixedTypeVariables)) {
                                    "An independent replay requested an unresolved shared binding"
                                }
                            }
                            system.registerVariable(variable)
                            system.fixVariable(variable, type,
                                org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition(variable))
                        }
                    } else {
                        // shared child 已在 export 前进入 active parent；只有独立 FULL 调用
                        // 的闭合存储需要在新的独立候选中导入。不能以旧 outer snapshot 覆盖 live CS。
                        check(retained.notFixedTypeVariables.isEmpty()) { "An independent retained call was not completed" }
                        system.integrateInferenceForest(listOf(retained), requireClosed = true)
                    }
                }
            }
        }

        systemInitialized = true
        system
    }

    /** 捕获的 base 规定 delta 起点；当前 PCLA 或已移交 graph 才规定实际 authority。 */
    private fun activeConstraintSystemOwner(system: ConstraintSystemImpl): ConstraintSystemImpl {
        // 独立 clone 以 captured source 为 authority；共享 outer CS 才由活动 PCLA/graph 接管。
        if (!system.usesOuterCs) return checkNotNull(system.parentConstraintSystemOwner)
        val scope = implicitTypeComputationScope
        if (scope?.mode == org.cangnova.cangjie.cfir.resolve.inference.CfirImplicitTypeComputationMode.FORMAL) {
            return checkNotNull(scope.graph.constraintSystemOwner)
        }
        val inference = bodyResolveContext.inferenceSession
        return if (inference is org.cangnova.cangjie.cfir.resolve.inference.CfirPCLAInferenceSession &&
            !inference.isOuterCandidate(this))
            inference.currentCommonSystem else checkNotNull(system.parentConstraintSystemOwner)
    }

    /** 每次新的语义操作消费最新 parent；实际 analyzer 和事务内部保持系统稳定。 */
    override val system: ConstraintSystemImpl
        get() = candidateSystem.also { system ->
            if (system.parentConstraintSystemOwner != null) system.synchronizeWithOwner(activeConstraintSystemOwner(system))
        }

    /**
     * FULL 写回及其只读凭证检查借用候选自己的实际 CS。
     * Kotlin writer 消费同一已分析系统；这里沿既有 common-system 借用协议阻止读取触发 parent rebase。
     * 不复制 CS、不修改 owner/VM/固定解；离开作用域后正常 system getter 仍同步最新 parent。
     */
    internal fun <R> withConstraintSystemForCompletionResults(action: (ConstraintSystemImpl) -> R): R {
        val completionSystem = candidateSystem
        return completionSystem.withParentRebaseSuspended { action(completionSystem) }
    }

    /** 当前候选约束系统中已经记录的约束错误。 */
    override val errors: List<ConstraintSystemError>
        get() = system.errors

    /**
     * Substitutor from declared type parameters to type variables created for that candidate
     */
    lateinit var substitutor: ConeSubstitutor
        private set

    /** 向 resolve 后语义检查暴露声明参数替换，不泄漏可变候选实现。 */
    override val typeParameterSubstitutorOrNull: ConeSubstitutor?
        get() = if (this::substitutor.isInitialized) substitutor else null
    /** 为候选声明类型参数创建的 fresh type variable 列表。 */
    lateinit var freshVariables: List<ConeTypeVariable>
        private set

    /**
     * 候选外部导入的、需要在同一次 completion 中固定的类型变量。
     *
     * 这类变量不属于当前 callable 声明的类型参数，例如模式推断创建的元素变量和
     * 无上下文 lambda 的形参变量。它们属于当前完成过程；若不把变量本身暴露给
     * completion，约束虽存在但不会进入 fixation 队列。
     */
    val additionalCompletionVariables: MutableSet<TypeConstructorMarker>
        get() = pclaCompletionState.additionalVariables

    /**
     * PCLA 中 fresh lambda receiver 的成员访问可能先得到一组 owner 候选。
     *
     * 当该集合尚未被后续语法信息收窄到单一 owner 时，resolver 仍可选择一个等价签名候选
     * 作为表达式代表，但不能把这个代表候选的 receiver 约束提交到 common system。
     */
    internal var freshReceiverConstraintToDrop: FreshReceiverConstraintToDrop? = null

    /**
     * 初始化声明类型参数到 fresh type variable 的 substitutor 和变量列表。
     */
    fun initializeSubstitutorAndVariables(substitutor: ConeSubstitutor, freshVariables: List<ConeTypeVariable>) {
        this.substitutor = substitutor
        this.freshVariables = freshVariables
    }

    /**
     * 更新候选 substitutor。
     */
    @UpdatingCandidateInvariants
    fun updateSubstitutor(substitutor: ConeSubstitutor) {
        this.substitutor = substitutor
    }

    // ---------------------------------------- Conversions ----------------------------------------

    /** callable reference 解析出的最终函数类型。 */
    var resultingTypeForCallableReference: ConeCangJieType? = null
        private set

    /** callable reference 的 adaptation 信息。 */
    internal var callableReferenceAdaptation: CallableReferenceAdaptation? = null
        private set

    /**
     * 初始化 callable reference adaptation 与结果类型。
     */
    internal fun initializeCallableReferenceAdaptation(
        callableReferenceAdaptation: CallableReferenceAdaptation?,
        resultingTypeForCallableReference: ConeCangJieType
    ) {
        require(this.callableReferenceAdaptation == null) { "callableReferenceAdaptation already initialized" }
        this.callableReferenceAdaptation = callableReferenceAdaptation
        this.resultingTypeForCallableReference = resultingTypeForCallableReference
        if (callableReferenceAdaptation != null) {
            numDefaults = callableReferenceAdaptation.defaults
        }
    }

    /**
     * Expressions in this set are arguments of the call that have function kind conversion applied (e.g., suspend conversion).
     */
    var argumentsWithFunctionKindConversion: HashSet<CfirExpression>? = null
        private set

    /**
     * 标记指定实参发生函数种类转换。
     */
    fun addFunctionKindConversionOfArgument(element: CfirExpression) {
        val set = argumentsWithFunctionKindConversion ?: HashSet<CfirExpression>().also { argumentsWithFunctionKindConversion = it }
        set += element
    }

    /** 实参到 SAM 转换信息的映射。 */
    var samConversionInfosOfArguments: HashMap<CfirExpression, CfirSamResolver.SamConversionInfo>? = null
        private set

    /**
     * 记录指定实参的 SAM 转换信息。
     */
    fun setSamConversionOfArgument(expression: CfirExpression, conversionInfo: CfirSamResolver.SamConversionInfo) {
        val map = samConversionInfosOfArguments
            ?: hashMapOf<CfirExpression, CfirSamResolver.SamConversionInfo>().also { samConversionInfosOfArguments = it }
        map[expression] = conversionInfo
    }

    // Computed getters

    /** 候选是否使用了 SAM 转换。 */
    val usesSamConversion: Boolean
        get() = samConversionInfosOfArguments != null

    /** 候选是否使用 SAM 转换或 SAM 构造器。 */
    val usesSamConversionOrSamConstructor: Boolean
        get() = usesSamConversion || symbol.origin == CfirDeclarationOrigin.SamConstructor

    /** 候选是否使用函数种类转换。 */
    val usesFunctionKindConversion: Boolean
        get() = argumentsWithFunctionKindConversion != null || callableReferenceAdaptation?.hasFunctionKindConversion() == true

    // ---------------------------------------- Argument mapping ----------------------------------------

    /** 候选原始实参 atom 列表。 */
    private var _arguments: List<ConeResolutionAtom>? = null
    /** 候选原始实参 atom 列表。 */
    val arguments: List<ConeResolutionAtom>
        get() = _arguments ?: error("Argument list is not initialized yet")

    /** 候选实参到值参数的映射。 */
    private var _argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>? = null
    /** 实参映射是否已经初始化。 */
    override val argumentMappingInitialized: Boolean
        get() = _argumentMapping != null
    /** 候选实参到值参数的映射。 */
    override val argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>
        get() = _argumentMapping ?: error("Argument mapping is not initialized yet")

    /** 参数映射阶段产生的共享结构化结果。 */
    var argumentMappingOutcome: ArgumentMappingOutcome? = null
        private set

    /** 不影响候选适用性、但必须在完成后报告的解析诊断。 */
    val nonBlockingResolutionDiagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()

    /** 当前候选普通变参形参。 */
    private var _variadicParameter: CfirValueParameter? = null
    /** 普通变参元素类型。 */
    private var _variadicElementType: ConeCangJieType? = null
    /** 固定位置实参数量。 */
    private var _variadicFixedPositionalArity: Int? = null
    /** 被标记为变参实参的 atom 到期望元素类型映射。 */
    private var _variadicArgumentExpectedTypes: LinkedHashMap<ConeResolutionAtom, ConeCangJieType>? = null
    /** 变参映射形状允许在类型检查阶段按元素重试的实参。 */
    private var _variadicEligibleArguments: Set<ConeResolutionAtom> = emptySet()

    /** 候选是否使用普通变参调用。 */
    val usesVariadicCall: Boolean
        get() = _variadicArgumentExpectedTypes?.isNotEmpty() == true ||
                (_variadicParameter != null && _variadicFixedPositionalArity == arguments.size)

    /**
     * 初始化候选实参列表和实参映射。
     */
    fun initializeArgumentMapping(
        arguments: List<ConeResolutionAtom>,
        argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>,
    ) {
        require(_argumentMapping == null) { "Argument mapping already initialized" }
        _argumentMapping = argumentMapping
        _arguments = arguments
    }

    /** 初始化参数映射的共享结构化结果。 */
    fun initializeArgumentMappingOutcome(outcome: ArgumentMappingOutcome) {
        require(argumentMappingOutcome == null) { "Argument mapping outcome already initialized" }
        argumentMappingOutcome = outcome
    }

    /** 记录不参与适用性排序的解析诊断。 */
    fun addNonBlockingResolutionDiagnostic(diagnostic: ResolutionDiagnostic) {
        nonBlockingResolutionDiagnostics += diagnostic
    }

    /**
     * 更新候选实参映射。
     */
    @UpdatingCandidateInvariants
    fun updateArgumentMapping(argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>) {
        _argumentMapping = argumentMapping
        _variadicArgumentExpectedTypes?.keys?.retainAll(argumentMapping.keys)
        _variadicEligibleArguments = _variadicEligibleArguments.intersect(argumentMapping.keys)
    }

    /**
     * 初始化普通变参调用信息。
     */
    fun initializeVariadicCallInfo(
        parameter: CfirValueParameter,
        elementType: ConeCangJieType,
        fixedPositionalArity: Int,
    ) {
        require(_variadicParameter == null || _variadicParameter == parameter) {
            "Variadic call info already initialized"
        }
        _variadicParameter = parameter
        _variadicElementType = elementType
        _variadicFixedPositionalArity = fixedPositionalArity
    }

    /**
     * 判断 atom 是否可以作为变参实参。
     */
    fun canUseVariadicArgument(atom: ConeResolutionAtom): Boolean =
        _variadicParameter != null && atom in _variadicEligibleArguments

    /** 提交 mapper 已确认可由变参形参承接的实参集合，不等同于实际采用变参语义。 */
    fun initializeVariadicEligibleArguments(arguments: Collection<ConeResolutionAtom>) {
        require(_variadicEligibleArguments.isEmpty()) { "Variadic eligible arguments already initialized" }
        _variadicEligibleArguments = arguments.toSet()
    }

    /**
     * 标记 atom 为变参实参，并返回元素期望类型。
     */
    fun markVariadicArgument(atom: ConeResolutionAtom): ConeCangJieType? {
        val elementType = _variadicElementType ?: return null
        val expectedTypes = _variadicArgumentExpectedTypes
            ?: LinkedHashMap<ConeResolutionAtom, ConeCangJieType>().also {
                _variadicArgumentExpectedTypes = it
            }
        expectedTypes[atom] = elementType
        activateCangjieVariadicCall()
        return elementType
    }

    /**
     * 标记空变参调用。
     */
    fun markEmptyVariadicCall() {
        require(_variadicParameter != null) { "Variadic call info is not initialized" }
        activateCangjieVariadicCall()
    }

    /**
     * 查询变参实参的期望类型。
     */
    fun variadicExpectedTypeForArgument(atom: ConeResolutionAtom): ConeCangJieType? =
        _variadicArgumentExpectedTypes?.get(atom)

    /**
     * 判断指定参数是否为当前候选的变参形参。
     */
    fun hasVariadicParameter(parameter: CfirValueParameter): Boolean =
        _variadicParameter == parameter

    /** 固定位置实参数量。 */
    val variadicFixedPositionalArity: Int?
        get() = _variadicFixedPositionalArity

    /**
     * 实参前缀替换后同步变参实参期望类型映射。
     */
    private fun remapVariadicArgumentExpectedTypes(
        oldToNewArguments: Map<ConeResolutionAtom, ConeResolutionAtom>,
        remainingArguments: List<ConeResolutionAtom>,
    ) {
        val oldExpectedTypes = _variadicArgumentExpectedTypes ?: return
        val newExpectedTypes = LinkedHashMap<ConeResolutionAtom, ConeCangJieType>()
        for ((oldArgument, newArgument) in oldToNewArguments) {
            oldExpectedTypes[oldArgument]?.let { newExpectedTypes[newArgument] = it }
        }
        for (argument in remainingArguments) {
            oldExpectedTypes[argument]?.let { newExpectedTypes[argument] = it }
        }
        _variadicArgumentExpectedTypes = newExpectedTypes.takeIf { it.isNotEmpty() }
    }

    /**
     * The arguments of a contextual implicit `invoke` candidate contain stub expressions for the implicitly passed
     * context arguments between the [MapArguments] and [CheckContextArguments] stages.
     *
     * These expressions are always the first in the [arguments] list.
     *
     * This function replaces these stub arguments with the given [newArgumentPrefix] and updates the [argumentMapping] accordingly.
     */
    @UpdatingCandidateInvariants
    fun replaceArgumentPrefix(newArgumentPrefix: List<ConeResolutionAtom>) {
        val remainingArguments = arguments.subList(newArgumentPrefix.size, arguments.size)

        val newArgumentMapping = LinkedHashMap<ConeResolutionAtom, CfirValueParameter>()
        val oldToNewArguments = LinkedHashMap<ConeResolutionAtom, ConeResolutionAtom>()
        for ((oldArgument, newArgument) in arguments.zip(newArgumentPrefix)) {
            newArgumentMapping[newArgument] = argumentMapping.getValue(oldArgument)
            oldToNewArguments[oldArgument] = newArgument
        }

        for (argument in remainingArguments) {
            argumentMapping[argument]?.let { newArgumentMapping[argument] = it }
        }

        val newArguments = newArgumentPrefix + remainingArguments

        _arguments = newArguments
        _argumentMapping = newArgumentMapping
        remapVariadicArgumentExpectedTypes(oldToNewArguments, remainingArguments)
        val oldEligibleArguments = _variadicEligibleArguments
        _variadicEligibleArguments = buildSet {
            for ((oldArgument, newArgument) in oldToNewArguments) {
                if (oldArgument in oldEligibleArguments) add(newArgument)
            }
            addAll(remainingArguments.filter { it in oldEligibleArguments })
        }
    }

    /** 调用中使用默认参数的数量。 */
    var numDefaults: Int = 0
    /** 候选是否使用 quest fallback。 */
    var usedQuestFallback: Boolean = false
    /** 候选是否使用理想数值兼容。 */
    var usedIdealNumericCompatibility: Boolean = false
    /** 候选是否通过 extend 参与解析。 */
    var usedExtendParticipation: Boolean = false

    /** 当前调用实际采用的仓颉变参形参。 */
    private var _cangjieVariadicParameterForCall: CfirValueParameter? = null

    /**
     * 本次调用实际采用的仓颉变参形参。
     *
     * 仓颉变参由调用解析按 `Array<T>` 形状解糖产生，不能只看声明形状；
     * 普通数组实参匹配同一个声明时不应被标记为变参调用。
     */
    val cangjieVariadicParameterForCall: CfirValueParameter?
        get() = _cangjieVariadicParameterForCall

    /** 候选是否使用仓颉变参调用。 */
    val usesCangjieVariadicCall: Boolean
        get() = _cangjieVariadicParameterForCall != null

    /** 在映射或类型检查确认采用元素收集语义后，激活实际变参调用。 */
    private fun activateCangjieVariadicCall() {
        val parameter = _variadicParameter ?: return
        if (_cangjieVariadicParameterForCall == null) {
            _cangjieVariadicParameterForCall = parameter
        } else {
            require(_cangjieVariadicParameterForCall == parameter) {
                "Cangjie variadic call was activated with a different parameter"
            }
        }
    }

    // ---------------------------------------- Type argument mapping ----------------------------------------

    /** 显式/隐式类型实参映射。 */
    lateinit var typeArgumentMapping: TypeArgumentMapping

    // ---------------------------------------- Postponed atoms ----------------------------------------

    /** 需要调用完成阶段分析的 postponed atom 列表。 */
    val postponedAtoms: MutableList<ConePostponedResolvedAtom> = mutableListOf()

    /**
     * 追加 postponed atom。
     */
    fun addPostponedAtom(atom: ConePostponedResolvedAtom) {
        postponedAtoms += atom
    }

    // ------------------------ Context-sensitively resolved arguments ------------------------------------

    /** 上下文敏感解析或集合字面量解析产生的实参替换映射。 */
    private var _updatedArguments: MutableMap<CfirElement, CfirExpression>? =
        null

    /**
     * 注册一个实参替换。
     */
    private fun setUpdatedArgument(old: CfirExpression, new: CfirExpression) {
        // 替换结果归当前候选创建时的推断图；producer 的临时解析 context 不能替代该 owner。
        // 保留真实 old -> new 关系，使 snapshot/reinfer 后消费的新节点仍能定位原 RAW site。
        implicitTypeComputationScope?.registerDerivedElement(old, new)
        if (_updatedArguments == null) {
            _updatedArguments = mutableMapOf()
        }

        val existingValue = _updatedArguments!!.put(old, new)
        check(existingValue == null) {
            "We shouldn't put the value for $old twice"
        }
    }

    /**
     * 记录上下文敏感解析后的简单名替换。
     */
    internal fun setUpdatedArgumentFromContextSensitiveResolution(
        old: CfirExpression,
        new: CfirExpression,
    ) {
        setUpdatedArgument(old, new)
    }

    /**
     * 记录集合字面量解析后的替换。
     */
    fun setUpdatedCollectionLiteral(old: CfirArrayLiteral, new: CfirExpression) {
        setUpdatedArgument(old, new)
    }

    /** 实参替换映射。 */
    val argumentReplacements: Map<CfirElement, CfirExpression>?
        get() = _updatedArguments

    // ---------------------------------------- PCLA-related parts ----------------------------------------

    /** PCLA completion 后需要写回的 postponed 调用。 */
    val postponedPCLACalls: MutableList<ConeResolutionAtom>
        get() = pclaCompletionState.calls
    /** 已经通过 PCLA 分析过的 lambda 声明。 */
    val lambdasAnalyzedWithPCLA: MutableList<CfirDeclaration>
        get() = pclaCompletionState.lambdas

    /** PCLA 完成结果写回回调；模式投影与子推断图在同一完成阶段发布最终类型。 */
    val onPCLACompletionResultsWritingCallbacks: MutableList<(ConeSubstitutor) -> Unit>
        get() = pclaCompletionState.callbacks

    /** 私有图调用保留各自的 writer/DFA；正式调用继续使用当前正文的 writer。 */
    internal val pclaCallWriters: IdentityHashMap<ConeResolutionAtom, (ConeResolutionAtom, ConeSubstitutor) -> Unit>
        get() = pclaCompletionState.callWriters

    /** body reinference 已放弃的候选只保留语义事实，不能继续持有源码写入权限。 */
    internal val isSourceCompletionWithdrawn: Boolean get() = sourceCompletionWithdrawn

    /** 由实际 producer 采用新 body 后调用，不按名字、类型或队列顺序推测替代候选。 */
    private fun withdrawSourceCompletion() {
        if (isSourceCompletionWithdrawn) return
        implicitTypeComputationScope?.let { scope -> scope.graph.withdrawCompletionCandidate(scope, this) }
        revokeUnscopedWriter()
        sourceCompletionWithdrawn = true
    }

    /** reset 更换分析代次；完整成功 memento 可恢复它，parent-only restore 不改 child 代次。 */
    internal val unscopedCompletionGeneration: Any get() = pclaCompletionState.unscopedAnalysisGeneration

    internal val unscopedWriterEpoch: Any get() = unscopedCompletionEpoch

    /** 水位与其实际 Candidate 队列同寿命，成功/回滚 memento 同时保存它们。 */
    internal var processedPostponedCallsCount: Int
        get() = pclaCompletionState.processedCallsCount
        set(value) {
            check(value in 0..postponedPCLACalls.size) { "Postponed call watermark is outside its owning queue" }
            pclaCompletionState.processedCallsCount = value
        }

    /** 任一正式消费都按删除前的真实位置同步水位；不通过上下文猜测 owning session。 */
    internal fun removePostponedCall(call: ConeResolutionAtom): Boolean {
        val removedBeforeWatermark = postponedPCLACalls.withIndex().count { (index, queued) ->
            index < processedPostponedCallsCount && queued === call
        }
        val removed = postponedPCLACalls.removeAll { it === call }
        if (removed) {
            pclaCallWriters.remove(call)
            processedPostponedCallsCount -= removedBeforeWatermark
        }
        return removed
    }

    /** 恢复任务/事实前先撤销全部 held writer；这个 epoch 不可由任何快照恢复。 */
    private fun revokeUnscopedWriter() {
        unscopedCompletionEpoch = Any()
        pclaCompletionState.unscopedPass = null
    }

    /** 只由显式 FULL factory 调用；队列对象仍由原 PCLA owner 持有。 */
    internal fun captureUnscopedCompletionPass(
        producer: org.cangnova.cangjie.cfir.resolve.inference.CfirFullCompletionAuthority,
    ): CfirUnscopedCompletionPass {
        // 新 epoch 只允许存活 owner/producer 创建新 pass，不能复活已替换的源码生产者。
        check(!isSourceCompletionWithdrawn && !producer.candidate.isSourceCompletionWithdrawn &&
            implicitTypeComputationScope?.graph?.isCandidateWithdrawn(this) != true &&
            producer.candidate.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(producer.candidate) != true
        ) { "Withdrawn source producer cannot create a completion pass" }
        return CfirUnscopedCompletionPass(this, producer, system).also { pclaCompletionState.unscopedPass = it }
    }

    internal fun isUnscopedCompletionPassActive(pass: CfirUnscopedCompletionPass): Boolean =
        !isSourceCompletionWithdrawn && pass.owner === this && pclaCompletionState.unscopedPass === pass &&
            pass.ownerSystem === withConstraintSystemForCompletionResults { it } &&
            pass.ownerGeneration === unscopedCompletionGeneration && pass.writerEpoch === unscopedWriterEpoch &&
            pass.matchesProducerFact() && pass.producer.writerEpoch === pass.producer.candidate.unscopedWriterEpoch &&
            implicitTypeComputationScope?.graph?.isCandidateWithdrawn(this) != true

    internal fun unscopedCompletionReceipt(atom: ConeAtomWithCandidate, system: ConstraintSystemImpl): CfirUnscopedCompletionReceipt? =
        pclaCompletionState.unscopedReceipts[atom]?.takeIf { it.isValidFor(this, atom, system) }

    internal fun hasActualUnscopedReplacement(expression: CfirExpression, previous: Candidate, system: ConstraintSystemImpl): Boolean {
        val atoms = postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>().filter {
            it.expression === expression && pclaCallWriters[it] == null
        }
        val live = atoms.filter { unscopedCompletionReceipt(it, system) == null }
        val actual = ((expression as? CfirResolvable)?.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate
        if (actual != null) {
            return !actual.isSourceCompletionWithdrawn && actual !== previous && actual.implicitTypeComputationScope == null &&
                live.size == 1 && live.single().candidate === actual
        }
        // 新 atom 先运行时，只有其真正 FULL receipt 能证明当前 resolved 结果；不能按 symbol/type 认领。
        return live.isEmpty() && atoms.any { atom ->
            atom.candidate !== previous && unscopedCompletionReceipt(atom, system)?.matchesCurrentResult() == true
        }
    }

    /** 只消耗这一 owner 的确切已完成 atom；不撤销 candidate，也不向新 candidate 转交 receipt。 */
    internal fun consumeUnscopedCompletionReceipt(atom: ConeAtomWithCandidate, receipt: CfirUnscopedCompletionReceipt) {
        check(pclaCompletionState.unscopedReceipts[atom] === receipt && receipt.entry.pass.owner === this)
        pclaCompletionState.unscopedReceipts.remove(atom)
        removePostponedCall(atom)
    }

    internal fun publishUnscopedCompletionReceipt(receipt: CfirUnscopedCompletionReceipt) {
        val entry = receipt.entry
        check(isUnscopedCompletionPassActive(entry.pass) && entry.pass.owner === this)
        check(postponedPCLACalls.any { it === entry.atom } && pclaCallWriters[entry.atom] == null)
        pclaCompletionState.unscopedReceipts[entry.atom] = receipt
    }

    internal val retainedChildSessions: MutableList<org.cangnova.cangjie.cfir.resolve.inference.CfirImplicitTypeComputation>
        get() = pclaCompletionState.childSessions

    /** 成功恢复并替换真实 body 后撤权；普通 reset 仍只重建此 owner 的队列和分析代次。 */
    internal fun resetLambdaBodyResolutionResults(previousBodyCandidates: Collection<Candidate>) {
        check(callInfo.callSite is CfirAnonymousFunctionExpression)
        // 身份已在实际树恢复前捕获；这里只撤销旧 body 的 producer，不改 CS/VM/固定解。
        for (previous in previousBodyCandidates) {
            check(previous !== this) { "Lambda reinference cannot withdraw its own accept producer" }
            previous.withdrawSourceCompletion()
        }
        resetLambdaBodyResolutionResults()
    }

    /**
     * synthetic lambda 重查时丢弃上一遍的表达式写回任务，保留已经收集的类型约束。
     * 官方 ResetLambdaForReinfer 同样会重建 body；旧 replacement 不能覆盖第二遍成功的解析结果。
     */
    internal fun resetLambdaBodyResolutionResults() {
        val lambda = callInfo.callSite as CfirAnonymousFunctionExpression
        _updatedArguments = null
        postponedPCLACalls.clear()
        onPCLACompletionResultsWritingCallbacks.clear()
        lambdasAnalyzedWithPCLA.clear()
        pclaCallWriters.clear()
        retainedChildSessions.clear()
        revokeUnscopedWriter()
        pclaCompletionState.unscopedAnalysisGeneration = Any()
        pclaCompletionState.unscopedReceipts.clear()
        processedPostponedCallsCount = 0
        lambdasAnalyzedWithPCLA += lambda.anonymousFunction
    }

    // ---------------------------------------- Applicability ----------------------------------------

    /** 当前候选所有诊断中的最低适用性。 */
    var lowestApplicability: CandidateApplicability = CandidateApplicability.RESOLVED
        private set

    /** 对外暴露的候选适用性，等于当前最低诊断适用性。 */
    override val applicability: CandidateApplicability
        get() = lowestApplicability

    /** 候选在各 resolution stage 中收集到的诊断。 */
    override val diagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()

    /**
     * 添加候选诊断并更新最低适用性。
     */
    fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        diagnostics += diagnostic
        if (diagnostic.applicability < lowestApplicability) {
            lowestApplicability = diagnostic.applicability
        }
    }

    /**
     * 恢复候选最低适用性。
     */
    internal fun restoreLowestApplicability(applicability: CandidateApplicability) {
        lowestApplicability = applicability
    }

    /**
     * Note that [lowestApplicability]`.isSuccess == true` doesn't imply [isSuccessful].
     *
     * This is because [lowestApplicability] is equal to the lowest [ResolutionDiagnostic.applicability] of all [diagnostics],
     * but in presence of more than one diagnostic, the lowest one can be successful while a higher one isn't, e.g., the combination
     * of [CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY] and [CandidateApplicability.RESOLVED_WITH_ERROR].
     *
     * Also see [org.jetbrains.kotlin.fir.resolve.transformers.CfirCallCompletionResultsWriterTransformer.toResolvedReference]
     * as it contains conditions that rely on subtle differences between the implementation of this property and
     * [org.jetbrains.kotlin.resolve.calls.tower.isSuccess].
     */
    val isSuccessful: Boolean
        get() = diagnostics.all { it.isSuccess } && (!systemInitialized || !system.hasContradiction)

    // ---------------------------------------- Receivers ----------------------------------------

    /** 最终选择的 extension receiver atom，初始值来自调用点给定 receiver。 */
    override var chosenExtensionReceiver: ConeResolutionAtom? = givenExtensionReceiver

    /** 上下文参数对应的 receiver/argument atom 列表。 */
    override var contextArguments: List<ConeResolutionAtom>? = null

    /**
     * In case `f: context(C..) (V) -> ..`, `f(e..)`, context values are still being introduced as a prefix of
     * regular arguments for `invoke` function.
     */
    var expectedContextParameterCountForInvoke: Int? = null

    /**
     * 返回 dispatch receiver 对应表达式。
     */
    fun dispatchReceiverExpression(): CfirExpression? {
        return dispatchReceiver?.expression
    }

    /**
     * 返回选中的 extension receiver 对应表达式。
     */
    fun chosenExtensionReceiverExpression(): CfirExpression? {
        return chosenExtensionReceiver?.expression
    }

    /**
     * 返回 context arguments 对应表达式列表。
     */
    fun contextArguments(): List<CfirExpression> {
        return contextArguments?.map { it.expression } ?: emptyList()
    }

    /** receiver source 是否已经更新。 */
    private var sourcesWereUpdated = false

    // In case of implicit receivers we want to update corresponding sources to generate correct offset. This method must be called only
    // once when candidate was selected and confirmed to be correct one.
    /**
     * 标记 receiver source 已更新。
     */
    fun updateSourcesOfReceivers() {
        if (sourcesWereUpdated) return
        sourcesWereUpdated = true
    }

    // This thing is mostly for a common fast-path optimization and should not affect the semantics once it's set to `true`
    /** synthetic call 是否已经把期望类型作为 equality 约束加入。 */
    var wasExpectedTypeAddedAsEqualityForSyntheticCall: Boolean = false
        private set

    /**
     * 标记 synthetic call 已经使用期望类型 equality 约束。
     */
    fun markWasExpectedTypeAddedAsEqualityForSyntheticCall() {
        wasExpectedTypeAddedAsEqualityForSyntheticCall = true
    }

    // ---------------------------------------- Backing field ----------------------------------------

    /** 候选是否有可见 backing field。 */
    var hasVisibleBackingField: Boolean = false

    // ---------------------------------------- Util ----------------------------------------

    /** 候选已通过的 resolution stage 数量。 */
    var passedStages: Int = 0

    /** 候选约束系统视图。 */
    val constraintSystem: ConstraintSystemImpl
        get() = system

    /** callable value 合成参数缓存。 */
    private var cachedSyntheticCallableValueParameters: List<CfirValueParameter>? = null
    /** 当前函数值调用为 fresh lambda 形参临时构造的函数类型形状。 */
    private var callableValueInvokeFunctionShape: ConeFunctionType? = null

    /**
     * 当前候选是否表示函数值调用，而不是直接调用一个函数声明。
     *
     * common invoke、函数类型 receiver 的 synthetic invoke，以及直接以变量符号承载的
     * callable value 都共享严格 arity 和普通位置实参语义。参数数量判断、变参识别和
     * trailing closure 映射必须统一消费这一结构属性，不能各自按候选来源局部推断。
     */
    val isCallableValueCall: Boolean
        get() = callInfo.candidateForCommonInvokeReceiver != null ||
                isSyntheticFunctionTypeInvoke() ||
                symbol.takeIf { it.isBound }?.cfir.isCallableValueDeclaration()

    /**
     * payload enum 调用的实参是否已经是未实例化泛型函数值引用。
     *
     * 这类实参的主错误属于函数引用自身：例如 `Time.Day(getTime)` 中 `getTime`
     * 缺少函数类型实参。外层 enum owner 因而无法从该错误实参取得稳定类型，但不能把
     * 同一个根因再次提升为 `Time.Day` 的泛型推断失败。无参 enum value（如 `None`）
     * 不属于函数声明，仍由 payload enum 的正常推断路径处理。
     */
    val hasBareGenericFunctionReferencePayloadArgument: Boolean
        get() = (this as AbstractCallCandidate<*>).hasBareGenericFunctionReferencePayloadArgument()

    /**
     * 当前候选是否已由函数类型语义接管。
     *
     * [isCallableValueCall] 还会覆盖任意变量候选，以便普通对象在初次调用失败后仍能尝试
     * `operator ()`。本属性只识别已确认的函数类型调用；这类调用的失败必须保留在函数值
     * 参数映射路径，不能再被隐式 invoke 恢复改写。
     */
    val isFunctionTypeCallableValueCall: Boolean
        get() {
            if (callInfo.candidateForCommonInvokeReceiver != null || isSyntheticFunctionTypeInvoke()) {
                return true
            }
            if (callableValueInvokeFunctionShape != null) return true

            val declaration = symbol.takeIf { it.isBound }?.cfir
                ?.takeIf { it.isCallableValueDeclaration() }
                as? CfirCallableDeclaration ?: return false
            val declaredType = declaration.returnTypeRef
                .resolvedConeTypeOrNull()
                ?.fullyExpandedType(callInfo.session)
                ?: return false
            return declaredType is ConeFunctionType ||
                    (declaredType as? ConeErrorType)
                        ?.delegatedType
                        ?.fullyExpandedType(callInfo.session) is ConeFunctionType
        }

    /** 函数类型 receiver 上合成的 `invoke` 候选。 */
    private fun isSyntheticFunctionTypeInvoke(): Boolean {
        val function = symbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return false
        if (function.origin != CfirDeclarationOrigin.Synthetic.FakeFunction) return false
        if (callInfo.name != OperatorNameConventions.INVOKE) return false
        return dispatchReceiverExpression()?.coneTypeOrNull?.fullyExpandedType(callInfo.session) is ConeFunctionType
    }

    /**
     * 记录函数值调用语法反推出的函数类型形状。
     *
     * 无上下文 lambda 形参最初只是 fresh type variable；在 `g(arg)` 这种调用中，
     * 形参映射必须先看到 `(Arg) -> R` 结构，才能把实参和返回约束并入同一轮 PCLA。
     */
    fun registerCallableValueInvokeFunctionShape(functionType: ConeFunctionType) {
        callableValueInvokeFunctionShape = functionType
        cachedSyntheticCallableValueParameters = null
    }

    /**
     * 返回实参映射使用的声明参数列表。
     */
    fun declaredParametersForMapping(): List<CfirValueParameter> {
        if (!symbol.isBound) return emptyList()
        return when (val declaration = symbol.cfir) {
            is CfirFunction -> declaration.valueParameters
            is CfirConstructor -> declaration.valueParameters
            is CfirEnumConstructor -> declaration.valueParameters
            is CfirVariable -> callableValueParametersForMapping(declaration)
            is CfirProperty -> callableValueParametersForMapping(declaration)
            else -> emptyList()
        }
    }

    /**
     * 计算候选替换后的返回类型。
     */
    fun substitutedReturnType(declaredReturnType: ConeCangJieType? = null): ConeCangJieType {
        val declaration = symbol.cfir
        val declared = declaredReturnType ?: when (declaration) {
            is CfirFunction -> declaration.returnTypeRef.resolvedConeTypeOrNull()
            is CfirConstructor -> declaration.returnTypeRef.resolvedConeTypeOrNull()
            is CfirEnumConstructor -> declaration.returnTypeRef.resolvedConeTypeOrNull()
            is CfirVariable -> callableValueReturnType(declaration)
            is CfirProperty -> callableValueReturnType(declaration)
            else -> null
        } ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved return type"))

        val substituted = if (::substitutor.isInitialized) substitutor.substituteOrSelf(declared) else declared
        return if (declaration is CfirFunction && declaration.origin == CfirDeclarationOrigin.Synthetic.FakeFunction) {
            substituted
        } else {
            bindThisTypeToCallSite(substituted)
        }
    }

    /**
     * 把候选声明返回类型中的 `This` 视图绑定到调用点。
     *
     * 对齐官方 `TypeChecker::TypeCheckerImpl::GetCallTy`
     * （`external/cangjie_compiler/src/Sema/TypeCheckCall.cpp`）：
     * - `instance.f()` 的结果类型是接收者表达式的静态类型；
     * - `SomeClass.staticF()` 的结果类型是 owner 类型；
     * - 类体内无限定调用仍保持当前类的 `This` 视图。
     *
     * 声明返回类型转成调用表达式类型的转换只在这里发生一次，body-resolve 与
     * completion 写回共用同一实现，避免任一路径漏掉 `This` 绑定。
     */
    fun bindThisTypeToCallSite(type: ConeCangJieType): ConeCangJieType {
        val thisTypeView = type.thisTypeViewOrNull() ?: return type
        if (callInfo.explicitReceiver != null) {
            // 官方 GetCallTy 在 MemberAccess 分支上区分 static：static 成员即使声明了
            // `This`，调用结果也退化为 owner 类型。
            if (isStaticCallable()) return thisTypeView.approximateThisTypeForDeclaration()
            return explicitCallSiteReceiverType() ?: thisTypeView
        }
        return containingClassThisType()
            ?: explicitCallSiteReceiverType()
            ?: thisTypeView
    }

    /**
     * 取得类型中承载的 `This` 视图。
     *
     * `This` 出现在非法位置时解析结果是携带 delegated 类型的错误类型；官方在这些位置
     * 由声明处报告诊断，调用结果仍按 owner/接收者类型继续，因此这里同样向下取 `This` 视图。
     */
    private fun ConeCangJieType.thisTypeViewOrNull(): ConeClassLikeType? = when {
        this is ConeClassLikeType && isThisType -> this
        this is ConeErrorType -> delegatedType?.thisTypeViewOrNull()
        else -> null
    }

    /** 候选是否为 static callable。 */
    private fun isStaticCallable(): Boolean {
        if (!symbol.isBound) return false
        return (symbol.cfir as? CfirCallableDeclaration)?.status?.isStatic == true
    }

    /** 显式接收者调用可绑定 `This` 的接收者类型。 */
    private fun explicitCallSiteReceiverType(): ConeCangJieType? =
        dispatchReceiverExpression()?.coneTypeOrNull
            ?: chosenExtensionReceiverExpression()?.coneTypeOrNull

    /**
     * 构造当前包含 class 的 `This` 类型。
     */
    private fun containingClassThisType(): ConeCangJieType? {
        val containingClass = callInfo.containingDeclarations
            .asReversed()
            .filterIsInstance<CfirClass>()
            .firstOrNull()
            ?: return null
        val classSymbol = containingClass.symbol as? CfirClassSymbol ?: return null
        val typeArguments = containingClass.typeParameters.map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        return classSymbol.constructThisType(typeArguments)
    }

    /**
     * 对 callable value 候选提取函数类型返回类型。
     *
     * synthetic 顶层 lambda 的形参在 PCLA 会话内读取时因 `usedOuterCs` 走完整候选路径：
     * 形参仍是 fresh 占位（无原始类型参数的推断变量）时必须按其自身类型引用取值，
     * 否则会被错误地包装成 `Unresolved return type`，让下游调用收到
     * `ErrorTypeInArguments` 并阻断逐语句固定。具体类型的形参与其他变量保持既有
     * 函数值投影行为。
     */
    private fun callableValueReturnType(declaration: CfirCallableDeclaration): ConeCangJieType? {
        val declaredReturnType = declaration.returnTypeRef.resolvedConeTypeOrNull()
        if (declaration is CfirValueParameter && declaredReturnType is ConeTypeVariableType) {
            return declaredReturnType
        }
        val functionType = callableValueInvokeFunctionShape
            ?: declaredReturnType?.recoverableFunctionTypeOrNull(callInfo.session)
            ?: return null
        consumeInvokeType(CfirInferenceVariableRole.INVOKE_RETURN, 0, functionType.returnType)
        return functionType.returnType
    }

    /**
     * 从显式或隐式 resolved type ref 读取 Cone 类型。
     */
    private fun CfirTypeRef.resolvedConeTypeOrNull(): ConeCangJieType? = when (this) {
        is CfirResolvedTypeRef -> coneType
        is ResolvedImplicitTypeRef -> typeRef.coneType
        else -> null
    }

    /**
     * 为 callable value 候选合成形参列表。
     */
    private fun callableValueParametersForMapping(declaration: CfirCallableDeclaration): List<CfirValueParameter> {
        cachedSyntheticCallableValueParameters?.let { return it }

        val functionType = callableValueInvokeFunctionShape
            ?: declaration.returnTypeRef.resolvedConeTypeOrNull()
                ?.recoverableFunctionTypeOrNull(callInfo.session)
        if (functionType == null) {
            cachedSyntheticCallableValueParameters = emptyList()
            return emptyList()
        }
        functionType.parameterTypes.forEachIndexed { index, type ->
            consumeInvokeType(CfirInferenceVariableRole.INVOKE_PARAMETER, index, type)
        }
        if (functionType.parameterTypes.isEmpty()) {
            cachedSyntheticCallableValueParameters = emptyList()
            return emptyList()
        }

        val syntheticParameters = functionType.parameterTypes.mapIndexed { index, parameterType ->
            val parameterName = Name.identifier("callableValueArg$index")
            val parameterSymbol = CfirValueParameterSymbol(CallableId(parameterName))
            buildValueParameter {
                source = declaration.source
                moduleData = declaration.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Error
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                isNamed = false
                dispatchReceiverType = null
                symbol = parameterSymbol
                containingDeclarationSymbol = symbol
                status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                returnTypeRef = buildResolvedTypeRef {
                    source = declaration.returnTypeRef.source
                    coneType = parameterType
                }
                name = parameterName
                defaultValue = null
            }
        }

        cachedSyntheticCallableValueParameters = syntheticParameters
        return syntheticParameters
    }

    /**
     * 工厂在具体函数类型上不再执行；这里消费实际 mapper/return projection 采用的类型。
     * 两个工厂分别拥有 callSite 与 receiver，二者各按 RAW/derived identity 定位。
     */
    private fun consumeInvokeType(role: CfirInferenceVariableRole, index: Int, type: ConeCangJieType) {
        val scope = implicitTypeComputationScope ?: return
        scope.graph.consumeExpressionType(scope, callInfo.callSite, role, index, type, system)
        dispatchReceiverExpression()?.let { receiver ->
            scope.graph.consumeExpressionType(scope, receiver, role, index, type, system)
        }
    }

    /** 只有变量和属性的值访问可以作为函数值；普通函数声明走直接调用路径。 */
    private fun CfirDeclaration?.isCallableValueDeclaration(): Boolean =
        this is CfirVariable || this is CfirProperty


    /**
     * Please avoid updating symbol in the candidate whenever it's possible.
     * The only case when currently it seems to be unavoidable is at
     * [org.jetbrains.kotlin.fir.resolve.transformers.CfirCallCompletionResultsWriterTransformer.refineSubstitutedMemberIfReceiverContainsTypeVariable]
     */
    @RequiresOptIn
    annotation class UpdatingCandidateInvariants

    // ---------------------------------------- hashcode/equals/toString ----------------------------------------

    /**
     * 候选相等性按候选符号判定。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Candidate

        if (symbol != other.symbol) return false

        return true
    }

    /**
     * 候选哈希值按符号计算。
     */
    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    /**
     * 渲染候选调试字符串。
     */
    override fun toString(): String {
        val okOrFail = if (isSuccessful) "OK" else "FAIL"
        val step = "$passedStages/${callInfo.callKind.resolutionSequence.size}"
        return "$okOrFail($step): $symbol"
    }
}

/** 返回候选在来源保留成员图中的稳定身份。 */
internal fun Candidate.lookupIdentity(): CfirCandidateLookupIdentity =
    CfirCandidateLookupIdentity(symbol, lookupProvenance)
