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

package org.cangnova.cangjie.cfir.resolve.inference

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.resolve.body.*
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.processCandidatesAndPostponedAtoms
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeSemiFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractResolveTransformerSubclassing
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableTypeConstructor
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.forEachType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.ReceiverConstraintPosition
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker
import org.cangnova.cangjie.type.model.defaultType

/**
 * fresh lambda receiver 成员访问在单个 owner 下暴露的可调用形状。
 *
 * 官方播种候选 owner 集时每个 owner 都伴随自己的成员签名；实参侧
 * `FilterSumUpperbound` 需要按「该成员在 owner 上的返回类型」逐 owner 淘汰，
 * 因此形状必须绑定成员名称与具体 owner，避免不同成员的返回类型互相污染过滤结果。
 */
data class FreshReceiverMemberAccessShape(
    /** 被访问成员的名称。 */
    val memberName: Name,
    /** 该成员所属的候选 owner 类型。 */
    val ownerType: ConeCangJieType,
    /** 按当前约束系统归一化后的成员返回类型。 */
    val returnType: ConeCangJieType,
)

/**
 * PCLA 推断会话。
 *
 * 该会话维护一个与外层候选共享的 common constraint system，
 * 让 postponed call / lambda body 能在候选试跑期间累积并最终提交约束。
 */
class CfirPCLAInferenceSession(
    /** 外层候选，最终会接收 PCLA 推断结果。 */
    private val outerCandidate: Candidate,
    /** 构造约束系统和访问会话类型上下文所需的推断组件。 */
    private val inferenceComponents: InferenceComponents,
    /**
     * 启用逐语句 postponed 处理时所属的 lambda（synthetic 顶层 lambda 的首轮与重算路径）。
     *
     * 官方 `ChkLambda` 对该类 lambda 按语句顺序分析并在 tyvar 有解时立即替换；
     * 非 null 时 [onStatementResolved] 会在每条语句完成后消化本语句排队的调用，
     * 把已有唯一解的形参占位写回具体类型。默认 null 保持既有批处理语义。
     */
    private val statementProcessingOwnerLambda: CfirAnonymousFunction? = null,
) : CfirInferenceSession(), SessionHolder {
    override val session: CfirSession get() = inferenceComponents.session
    /** 可独立切换的 PCLA 推断系统与投影；调用队列和水位由实际 outer Candidate 持有。 */
    private class ComputationState(
        var commonSystem: ConstraintSystemImpl,
        val semiFixedVariables: MutableMap<TypeConstructorMarker, ConeCangJieType>,
        val projections: IdentityHashMap<org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner, (ConeSubstitutor) -> Unit>,
        val receiverOwners: LinkedHashMap<TypeConstructorMarker, MutableList<ConeCangJieType>>,
        val receiverShapes: LinkedHashMap<TypeConstructorMarker, MutableList<FreshReceiverMemberAccessShape>>,
    )

    private var computationState = ComputationState(
        prepareSharedBaseSystem(outerCandidate.system, inferenceComponents),
        linkedMapOf(), IdentityHashMap(), linkedMapOf(), linkedMapOf(),
    )

    init {
        // 新会话只消费本会话之后入队的调用；后续删除和 snapshot 恢复沿同一 Candidate 状态执行。
        outerCandidate.processedPostponedCallsCount = outerCandidate.postponedPCLACalls.size
    }

    /** 当前 PCLA 共享 common constraint system。 */
    var currentCommonSystem: ConstraintSystemImpl
        get() = computationState.commonSystem
        private set(value) { computationState.commonSystem = value }

    /** 外层候选是本会话 common system 的源 owner，不能反过来把自己的派生系统当父系统。 */
    internal fun isOuterCandidate(candidate: Candidate): Boolean = outerCandidate === candidate

    /** 此 infer owner 的实际待完成 atom；不从函数名或已完成类型寻找另一 owner。 */
    internal fun ownsMutableSourceCall(candidate: Candidate, call: CfirExpression): Boolean {
        if (outerCandidate.isSourceCompletionWithdrawn ||
            outerCandidate.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(outerCandidate) == true
        ) return false
        return outerCandidate === candidate || outerCandidate.postponedPCLACalls.any { atom ->
            atom is ConeAtomWithCandidate && atom.candidate === candidate && atom.expression === call
        }
    }

    /** 原 owner 的 FULL ledger 与已注册 child graph；读取完成事实不消耗队列或继承 writer capability。 */
    internal fun hasCompletedSourceCallResult(call: CfirExpression): Boolean {
        val ownResult = outerCandidate.withExistingConstraintSystemForCompletionResults { system ->
            outerCandidate.hasCompletedUnscopedSourceCall(call, system)
        } == true
        if (ownResult) return true
        if (outerCandidate.isSourceCompletionWithdrawn ||
            outerCandidate.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(outerCandidate) == true
        ) return false
        return outerCandidate.retainedChildSessions.any { graph ->
            graph.inferenceOwner === this && graph.completionOwnerCandidate === outerCandidate &&
                graph.hasCompletedSourceCallResult(call)
        }
    }

    /** 待 completion 的模式投影以真实 owner 去重，不能按绑定名称或源码文本合并。 */
    private val patternBindingProjections
        get() = computationState.projections

    override val semiFixedVariables: Map<TypeConstructorMarker, org.cangnova.cangjie.type.model.CangJieTypeMarker>
        get() = computationState.semiFixedVariables

    override fun getAndSemiFixCurrentResultIfTypeVariable(type: ConeCangJieType): ConeCangJieType? =
        semiFixCurrentResultIfTypeVariableAndReturnBinding(type, currentCommonSystem)?.second

    override fun semiFixTypeVariablesAllowingFixationToOtherOnes(type: ConeCangJieType, myCs: ConstraintSystemImpl) {
        type.forEachType { internalType ->
            semiFixCurrentResultIfTypeVariableAndReturnBinding(internalType, myCs, allowFixationToOtherTypeVariables = true)
        }
    }

    /** 对齐 Kotlin PCLA：求代表类型并添加 equality，不调用完整变量固定。 */
    fun semiFixCurrentResultIfTypeVariableAndReturnBinding(
        type: ConeCangJieType,
        myCs: ConstraintSystemImpl,
        allowFixationToOtherTypeVariables: Boolean = false,
    ): Pair<ConeTypeVariableTypeConstructor, ConeCangJieType>? {
        val constructor = (type as? ConeTypeVariableType)?.typeConstructor ?: return null
        require(constructor in myCs.allTypeVariables) { "$constructor not found" }
        val constraints = myCs.notFixedTypeVariables[constructor] ?: return null
        val result = myCs.prepareContextForTypeVariableForSemiFixation(
            constructor,
            allowFixationToOtherTypeVariables,
        ) {
            with(myCs) {
                inferenceComponents.resultTypeResolver.findResultType(
                    constraints, TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN,
                ).asCone()
            }
        } ?: return null
        val variable = constraints.typeVariable
        with(myCs) {
            addEqualityConstraint(variable.defaultType(), result, ConeSemiFixVariableConstraintPosition(variable))
        }
        computationState.semiFixedVariables[constructor] = result
        return constructor to result
    }

    /** proper 约束检查与代表类型求解使用同一个临时上下文，采用现行 Kotlin PCLA 的变量范围。 */
    private fun ConstraintSystemCompletionContext.prepareContextForTypeVariableForSemiFixation(
        coneTypeVariableTypeConstructor: ConeTypeVariableTypeConstructor,
        allowFixationToOtherTypeVariables: Boolean,
        resultTypeCallback: () -> ConeCangJieType?,
    ): ConeCangJieType? = withTypeVariablesThatAreCountedAsProperTypes(
        notFixedTypeVariables.keys,
        allowSemiFixationToOtherTypeVariables = allowFixationToOtherTypeVariables,
    ) {
        if (!inferenceComponents.variableFixationFinder.typeVariableHasProperConstraint(coneTypeVariableTypeConstructor)) {
            return@withTypeVariablesThatAreCountedAsProperTypes null
        }
        resultTypeCallback()
    }


    /** 把模式投影接入现有 PCLA writer 回调，并保留用于语句边界刷新的同一任务。 */
    internal fun registerPatternBindingProjection(
        owner: org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner,
        projection: (ConeSubstitutor) -> Unit,
    ) {
        val previous = patternBindingProjections.put(owner, projection)
        if (previous != null) outerCandidate.onPCLACompletionResultsWritingCallbacks.remove(previous)
        outerCandidate.onPCLACompletionResultsWritingCallbacks += projection
    }

    private fun refreshPatternBindingProjections() {
        // 候选事务会恢复 writer callbacks；登记表必须同步退役已被事务移除的投影任务。
        patternBindingProjections.entries.removeIf { it.value !in outerCandidate.onPCLACompletionResultsWritingCallbacks }
        if (patternBindingProjections.isEmpty()) return
        val substitutor = currentCommonSystem.currentStorage()
            .buildCurrentSubstitutor(inferenceComponents.session.typeContext, semiFixedVariables).asCone()
        patternBindingProjections.values.toList().forEach { it(substitutor) }
    }

    /** 读取共享系统已经固定的类型，不从模式语法制造新的 selector 形状。 */
    internal fun substituteCurrentType(type: ConeCangJieType): ConeCangJieType =
        currentCommonSystem.currentStorage()
            .buildCurrentSubstitutor(inferenceComponents.session.typeContext, semiFixedVariables).asCone()
            .substituteOrSelf(type)
    /** 当前 PCLA common system 是否已经出现约束矛盾。 */
    override val hasCurrentConstraintContradiction: Boolean
        get() = currentCommonSystem.hasContradiction
    /** fresh lambda receiver 对应的候选 owner 集合。 */
    private val freshReceiverCandidateOwnersByTypeVariable
        get() = computationState.receiverOwners
    /** fresh lambda receiver 成员访问记录的「owner -> 成员返回类型」形状。 */
    private val freshReceiverMemberShapesByTypeVariable
        get() = computationState.receiverShapes
    /** 逐语句处理已消费的 postponed 调用队列水位；从会话创建时的队列长度起步，只消化本会话新增的调用。 */
    private var processedPostponedCallsCount: Int
        get() = outerCandidate.processedPostponedCallsCount
        set(value) { outerCandidate.processedPostponedCallsCount = value }

    /** 返回当前候选应使用的共享基础约束存储。 */
    override fun baseConstraintStorageForCandidate(
        candidate: Candidate,
        bodyResolveContext: BodyResolveContext,
    ): ConstraintStorage? {
        if (candidate.mightBeAnalyzedAndCompletedIndependently(bodyResolveContext)) return null
        return currentCommonSystem.captureStorage()
    }

    /** 对使用外层约束系统的 postponed call 改用 PCLA postponed completion。 */
    override fun customCompletionModeInsteadOfFull(
        call: CfirResolvable,
    ): ConstraintSystemCompletionMode? = when {
        call.candidate()?.usedOuterCs == true -> ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL
        else -> null
    }

    /** 处理部分解析调用，并在需要时把候选系统写回 common system。 */
    override fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : CfirResolvable, T : CfirExpression {
        call.updateReturnTypeWithCurrentSubstitutor(resolutionMode)

        val candidate = call.candidate()
        if (candidate?.usedOuterCs != true) return
        currentCommonSystem.integrateDerivedStorage(candidate.system.captureStorage())
        candidate.freshReceiverConstraintToDrop?.let { constraintToDrop ->
            currentCommonSystem.removeConstraintsForVariable(constraintToDrop.receiverTypeConstructor) { constraint ->
                val position = constraint.position.from
                position is ReceiverConstraintPosition<*> &&
                    position.argument === constraintToDrop.receiverExpression
            }
            candidate.freshReceiverConstraintToDrop = null
        }

        // 仓颉无上下文 lambda 的暂定 writer 会先改写正文中的引用；声明 initializer 中的
        // PARTIAL 调用未必是外层 lambda 的返回 atom。共享系统必须保留实际调用和候选，
        // 才能在最终完成时继续遍历其泛型变量及子调用，不能从已解析的引用反查候选。
        if (completionMode == ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL ||
            completionMode == ConstraintSystemCompletionMode.PARTIAL
        ) {
            if (outerCandidate.postponedPCLACalls.none { atom ->
                    atom is ConeAtomWithCandidate && atom.expression === call && atom.candidate === candidate
                }
            ) outerCandidate.postponedPCLACalls += ConeAtomWithCandidate(call, candidate)
        }
        refreshPatternBindingProjections()
    }

    /** 在 PCLA 共享系统或候选系统中运行 lambda completion。 */
    override fun runLambdaCompletion(
        candidate: Candidate,
        forOverloadByLambdaReturnType: Boolean,
        block: () -> Unit,
    ): ConstraintStorage? {
        if (forOverloadByLambdaReturnType) {
            val constraintAccumulatorForLambda = inferenceComponents.createConstraintSystem().apply {
                setBaseSystem(currentCommonSystem.captureStorage())
            }

            runWithSpecifiedCurrentCommonSystem(constraintAccumulatorForLambda, block)
            return constraintAccumulatorForLambda.currentStorage()
        }

        runWithSpecifiedCurrentCommonSystem(candidate.system, block)
        return null
    }

    /** 临时切换当前 common system 执行 [block]。 */
    private fun <T> runWithSpecifiedCurrentCommonSystem(newSystem: ConstraintSystemImpl, block: () -> T): T {
        val previous = currentCommonSystem
        return try {
            currentCommonSystem = newSystem
            newSystem.withParentRebaseSuspended(block)
        } finally {
            currentCommonSystem = previous
        }
    }

    /** 将 PCLA common system 的最终结果提交回外层候选。 */
    fun applyResultsToMainCandidate() {
        outerCandidate.system.integrateDerivedStorage(currentCommonSystem.captureStorage(), protectParent = false)
    }

    /** 实际 PCLA owner 保留 source 完成任务；getter view 恢复不会改变队列身份。 */
    internal fun registerResourceSourceCompletion(task: CfirResourceSourceCompletion) {
        task.bindOwner(outerCandidate)
    }

    /** 对齐 Kotlin PCLA 的 child-session 移交入口，childStorage 必须来自当前 live parent。 */
    fun integrateChildSession(
        childCalls: Collection<ConeResolutionAtom>,
        childStorage: ConstraintStorage,
        onCompletionResultsWriting: (ConeSubstitutor) -> Unit,
    ) {
        // snapshot 的真实 baseline 才是派生关系证明；父新增、固定、删除和 settled fork 先 rebase。
        currentCommonSystem.integrateDerivedStorage(childStorage)
        outerCandidate.postponedPCLACalls += childCalls
        outerCandidate.onPCLACompletionResultsWritingCallbacks += onCompletionResultsWriting
    }

    /** 新 child 的 AST/candidate/队列独立；父 receiver authority 仅按 constructor identity 复制。 */
    internal fun createChildSession(
        candidate: Candidate,
        statementProcessingOwnerLambda: CfirAnonymousFunction? = null,
    ): CfirPCLAInferenceSession =
        CfirPCLAInferenceSession(candidate, inferenceComponents, statementProcessingOwnerLambda).also { child ->
            child.copyAuthorityFrom(this)
        }

    private fun copyAuthorityFrom(parent: CfirPCLAInferenceSession) {
        computationState.semiFixedVariables.putAll(parent.computationState.semiFixedVariables)
        parent.computationState.receiverOwners.forEach { (constructor, owners) ->
            computationState.receiverOwners[constructor] = owners.toMutableList()
        }
        parent.computationState.receiverShapes.forEach { (constructor, shapes) ->
            computationState.receiverShapes[constructor] = shapes.toMutableList()
        }
    }

    /** 私有晚写回保留自身 DFA，并显式取得此刻实际 owner 的共享系统与 authority。 */
    internal fun <T> withLiveParentSystem(parent: CfirPCLAInferenceSession, block: () -> T): T {
        copyAuthorityFrom(parent)
        return runWithSpecifiedCurrentCommonSystem(parent.outerCandidate.system, block)
    }

    internal fun <T> withActualCompletionSystem(system: ConstraintSystemImpl, block: () -> T): T =
        runWithSpecifiedCurrentCommonSystem(system, block)

    /**
     * 在实际 FORMAL writer 捕获 lease 之前结束注册 PRIVATE 生产。
     * calls 与 additional work 以实际 owner/graph/dispatcher 归属闭合；ordinary/Source FIFO 保持原样。
     */
    internal fun completeRetainedPrivateCallProducers(
        owner: Candidate,
        system: ConstraintSystemImpl,
        substitutor: ConeSubstitutor,
    ) {
        owner.withConstraintSystemForCompletionResults { check(isOuterCandidate(owner) && system === it) }
        val dispatched = java.util.Collections.newSetFromMap(IdentityHashMap<ConeResolutionAtom, Boolean>())
        val dispatchedAdditional = java.util.Collections.newSetFromMap(
            IdentityHashMap<CfirRetainedAdditionalResultsProducer, Boolean>(),
        )
        while (true) {
            val registeredGraphs = owner.retainedChildSessions.toList()
            val call = owner.postponedPCLACalls.firstOrNull { queued ->
                registeredGraphs.any { it.ownsRegisteredPrivateCallProducer(owner, queued) }
            }
            if (call != null) {
                val candidate = (call as ConeAtomWithCandidate).candidate
                val graph = checkNotNull(candidate.implicitTypeComputationScope).graph
                check(graph.inferenceOwner === this && dispatched.add(call)) {
                    "Retained PRIVATE producer changed its actual registration owner or was rescheduled recursively"
                }
                graph.completeRetainedPrivateCallProducer(owner, call, system, substitutor)
                continue
            }
            registeredGraphs.filter { it.inferenceOwner === this && it.completionOwnerCandidate === owner }
                .forEach { it.ensurePrivateAdditionalResultsProducer(owner, system) }
            val additional = owner.onPCLACompletionResultsWritingCallbacks
                .filterIsInstance<CfirRetainedAdditionalResultsProducer>()
                .firstOrNull { producer ->
                    registeredGraphs.any { it.ownsRegisteredPrivateAdditionalResultsProducer(owner, producer) }
                } ?: break
            check(additional.graph.inferenceOwner === this && dispatchedAdditional.add(additional)) {
                "PRIVATE additional producer changed its actual owner or was rescheduled recursively"
            }
            additional.complete(system, substitutor)
        }
    }

    /** retained 队列消费同步更新逐语句水位，不能因此漏掉之后新入队的真实调用。 */
    internal fun consumeRetainedCallProducer(owner: Candidate, call: ConeResolutionAtom) {
        check(isOuterCandidate(owner))
        owner.removePostponedCall(call)
    }

    /** scoped memento 记录该真实调用在本会话水位中的位置。 */
    internal fun isRetainedCallAlreadyProcessed(owner: Candidate, index: Int): Boolean {
        check(isOuterCandidate(owner) && index in owner.postponedPCLACalls.indices)
        return index < processedPostponedCallsCount
    }

    /** 只恢复指定调用槽及其注册 writer；兄弟队列与普通 callbacks 保持原对象。 */
    internal fun restoreRetainedCallProducer(
        owner: Candidate,
        index: Int,
        call: ConeResolutionAtom,
        writer: (ConeResolutionAtom, ConeSubstitutor) -> Unit,
        wasProcessed: Boolean,
    ) {
        check(isOuterCandidate(owner) && owner.postponedPCLACalls.none { it === call })
        val restoredIndex = index.coerceAtMost(owner.postponedPCLACalls.size)
        owner.postponedPCLACalls.add(restoredIndex, call)
        owner.pclaCallWriters[call] = writer
        if (wasProcessed && restoredIndex <= processedPostponedCallsCount) {
            processedPostponedCallsCount++
        } else if (!wasProcessed) {
            processedPostponedCallsCount = minOf(processedPostponedCallsCount, restoredIndex)
        }
    }

    internal fun integrateRetainedChildSession(
        graph: CfirImplicitTypeComputation,
        child: CfirPCLAInferenceSession,
        childCandidate: Candidate,
        writeCall: (ConeResolutionAtom, ConeSubstitutor) -> Unit,
        writeAdditionalResults: (List<(ConeSubstitutor) -> Unit>, List<CfirDeclaration>, ConeSubstitutor) -> Unit,
    ) {
        val results = childCandidate.capturePCLACompletionState()
        val dependencySet = java.util.Collections.newSetFromMap(IdentityHashMap<CfirImplicitTypeComputation, Boolean>())
        dependencySet.addAll(results.childSessions)
        val collectCandidate: (Candidate) -> Unit = { dependencySet.addAll(it.retainedChildSessions) }
        results.calls.forEach { it.processCandidatesAndPostponedAtoms(collectCandidate, {}) }
        graph.selectedCandidates().forEach { it.processCandidatesAndPostponedAtoms(collectCandidate, {}) }
        val dependencies = dependencySet.toList()
        val dependencyCalls = dependencies.flatMapTo(hashSetOf()) { it.ownCalls }
        val dependencyCallbacks = dependencies.mapNotNullTo(hashSetOf()) { it.completionCallback }
        val calls = results.calls.filter { it !in dependencyCalls }
        val sourceCallbacks = results.callbacks.filterIsInstance<CfirResourceSourceCompletion>()
        val callbacks = results.callbacks.filter { it !in dependencyCallbacks && it !is CfirResourceSourceCompletion }
        graph.retainResults(calls, callbacks, results.lambdas, emptyList())
        graph.retainVariableOrigins(childCandidate.system.currentStorage(), dependencies)

        graph.writeCall = { call, substitutor ->
            val actualSystem = checkNotNull(graph.completionOwnerCandidate).system
            // metadata 与 child context 同时指向真实最终 owner；VM/fixed binding 仍由 writer 严格核验。
            graph.constraintSystemOwner = actualSystem
            graph.withPrivateOrigins {
                child.withActualCompletionSystem(actualSystem) { writeCall(call, substitutor) }
            }
        }
        val completionWriter:
                (List<(ConeSubstitutor) -> Unit>, List<CfirDeclaration>, ConeSubstitutor) -> Unit = { callbacks, lambdas, substitutor ->
            graph.withPrivateOrigins {
                child.withActualCompletionSystem(checkNotNull(graph.completionOwnerCandidate).withConstraintSystemForCompletionResults { it }) {
                    writeAdditionalResults(callbacks, lambdas, substitutor)
                }
            }
        }
        val completion = graph.registerPrivateAdditionalResultsProducer(outerCandidate, completionWriter)
        integrateChildSession(results.calls, childCandidate.system.captureStorage(), completion)
        sourceCallbacks.forEach(::registerResourceSourceCompletion)
        outerCandidate.onPCLACompletionResultsWritingCallbacks += results.callbacks.filter { it in dependencyCallbacks }
        outerCandidate.pclaCallWriters.putAll(results.callWriters)
        for (call in calls) outerCandidate.pclaCallWriters[call] = checkNotNull(graph.writeCall)
        outerCandidate.retainedChildSessions += dependencies
        outerCandidate.retainedChildSessions += graph
        for (retained in dependencies + graph) {
            retained.inferenceOwner = this
            retained.constraintSystemOwner = currentCommonSystem
            retained.completionOwnerCandidate = outerCandidate
            retained.rebindPrivateAdditionalResultsProducerOwner(outerCandidate)
        }
        outerCandidate.additionalCompletionVariables += graph.ownVariableConstructors
        computationState.semiFixedVariables.putAll(child.computationState.semiFixedVariables)
        child.computationState.receiverOwners.forEach { (constructor, owners) ->
            computationState.receiverOwners[constructor] = owners.toMutableList()
        }
        child.computationState.receiverShapes.forEach { (constructor, shapes) ->
            computationState.receiverShapes[constructor] = shapes.toMutableList()
        }
    }

    /** 只退役对应私有任务；不撤销已移交约束、不删除变量或另一个资源图的任务。 */
    internal fun retireChildSession(graph: CfirImplicitTypeComputation) {
        val calls = graph.ownCalls.toSet()
        calls.forEach { outerCandidate.removePostponedCall(it) }
        graph.completionCallback?.let { callback ->
            outerCandidate.onPCLACompletionResultsWritingCallbacks.removeAll { it === callback }
        }
        outerCandidate.retainedChildSessions.removeAll { it === graph }
    }

    /**
     * 从已排队 postponed 调用中提取 fresh 形参的已知类型界。
     *
     * 每个排队调用的参数映射里，凡以该 receiver 变量为实参的参数位都给出一个
     * `变量 <: 参数类型` 上界（按该候选自己的当前替换结果换算）；候选系统内已记录
     * 的显式约束与固定解也一并返回。
     * 队列中的候选可已完成；按实际系统快照读取该候选的约束和固定解。
     */
    override fun knownBoundsForFreshReceiver(
        receiverTypeConstructor: TypeConstructorMarker,
    ): List<ConeCangJieType> {
        val bounds = mutableListOf<ConeCangJieType>()
        for (atom in outerCandidate.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>()) {
            val candidate = atom.candidate
            // 「自身就是对该 fresh receiver 的成员访问」的排队调用不提供硬界：
            // 其 argumentMapping 与 receiver 约束表达的是 owner-sum 集合事实
            // （官方 TryInitializeBaseSum 的候选 owner 集合），不是类型硬界；
            // 当作硬界会在 `{x => x.v30_2; x.v30_3}` 这类多 owner 交集场景误杀合法候选。
            if (candidate.isFreshReceiverMemberAccessOn(receiverTypeConstructor)) continue
            val storage = candidate.system.captureStorage()
            if (candidate.argumentMappingInitialized) {
                val substitutor = storage
                    .buildCurrentSubstitutor(inferenceComponents.session.typeContext, emptyMap())
                    .asCone()
                for ((argument, parameter) in candidate.argumentMapping) {
                    val argumentType = argument.expression.coneTypeOrNull as? ConeTypeVariableType ?: continue
                    if (argumentType.typeConstructor != receiverTypeConstructor) continue
                    val parameterType = parameter.returnTypeRef.coneTypeOrNull?.let { declared ->
                        substitutor.substituteOrNull(declared) ?: declared
                    } ?: continue
                    if (parameterType is ConeErrorType) continue
                    bounds += parameterType
                }
            }

            val constraints = storage
                .notFixedTypeVariables[receiverTypeConstructor]?.constraints.orEmpty()
            for (constraint in constraints) {
                if (constraint.kind != ConstraintKind.UPPER &&
                    constraint.kind != ConstraintKind.LOWER &&
                    constraint.kind != ConstraintKind.EQUALITY
                ) continue
                val constraintType = constraint.type as? ConeCangJieType ?: continue
                if (constraintType is ConeErrorType) continue
                bounds += constraintType
            }
            (storage.fixedTypeVariables[receiverTypeConstructor]
                as? ConeCangJieType)?.let { fixedType ->
                if (fixedType !is ConeErrorType) bounds += fixedType
            }
        }
        return bounds.distinctBy { it.toString() }
    }

    /** 判断排队调用的 dispatch/extension receiver 是否正是查询中的 fresh lambda 形参变量。 */
    private fun Candidate.isFreshReceiverMemberAccessOn(
        receiverTypeConstructor: TypeConstructorMarker,
    ): Boolean {
        val receiverExpression = dispatchReceiver?.expression
            ?: givenExtensionReceiver?.expression
            ?: return false
        val receiverType = receiverExpression.coneTypeOrNull as? ConeTypeVariableType ?: return false
        return receiverType.typeConstructor == receiverTypeConstructor
    }

    /**
     * synthetic 顶层 lambda 的逐语句 postponed 处理。
     *
     * 每条语句解析完成后消化该语句排队的 postponed 调用：从新增调用的 argumentMapping
     * 提取「形参变量 -> 参数类型」硬界，对已有唯一解的形参占位按官方 `ChkLambda`
     * 「tyvar 有解立即替换」语义写回具体类型，使后续语句的成员查找直接按具体类型解析。
     */
    override fun onStatementResolved(statement: CfirStatement) {
        refreshPatternBindingProjections()
        if (statementProcessingOwnerLambda == null) return

        val queueSize = outerCandidate.postponedPCLACalls.size
        val fromIndex = processedPostponedCallsCount
        check(fromIndex in 0..queueSize) { "Postponed call watermark is outside its owning queue" }
        processedPostponedCallsCount = queueSize
        val newAtoms = outerCandidate.postponedPCLACalls
            .subList(fromIndex, queueSize)
            .filterIsInstance<ConeAtomWithCandidate>()
        for (parameter in statementProcessingOwnerLambda.valueParameters) {
            fixLambdaParameterFromHardBounds(parameter, newAtoms)
        }
        refreshPatternBindingProjections()
    }

    /**
     * 用语句边界内累积的硬界求解并写回 lambda 形参类型引用。
     *
     * 只处理仍为 fresh 占位的形参；多个硬界迭代求交，交集失败、解含未固定推断变量或与
     * common system 既有约束冲突时放弃固定，交由既有诊断路径报告真实错误。
     */
    private fun fixLambdaParameterFromHardBounds(
        parameter: CfirValueParameter,
        newAtoms: List<ConeAtomWithCandidate>,
    ) {
        val placeholderType = parameter.returnTypeRef.coneTypeOrNull as? ConeTypeVariableType ?: return
        val variableConstructor = placeholderType.typeConstructor
        // 外层调用的实参若是对该形参变量的成员访问（如 g29(x.v) 的 x.v），实参类型不是裸
        // tyvar，硬界提取拿不到信息；官方此时用外层被调形参类型执行 FilterSumUpperbound
        // 收窄 owner-sum，这里先消化同一证据再做硬界求解。
        refineOwnersFromOuterParameterBounds(variableConstructor, newAtoms)
        val hardBounds = collectHardBoundsForVariable(variableConstructor, newAtoms) +
                collectProperDirectConstraintBounds(variableConstructor) +
                convergedFreshReceiverOwnerBounds(variableConstructor)
        if (hardBounds.isEmpty()) return
        val solution = solveUniqueSolution(variableConstructor, hardBounds) ?: return
        parameter.replaceReturnTypeRef(
            solution.toCfirResolvedTypeRef(parameter.returnTypeRef.source, parameter.returnTypeRef),
        )
    }

    /**
     * 读取当前 common system 中直接挂在 lambda 形参上的可用上界。
     *
     * 模式 initializer 的约束不经过 postponed call 的 argumentMapping：例如
     * `let (Some(x), y) <- (a, b)` 会直接产生 `a <: Option<T>`。这里不能只看调用
     * 队列，否则这种约束永远到不了形参定型路径。先使用当前 substitutor 替换已经
     * 求出的嵌套变量；只有替换后成为 proper type 的约束才可以作为本语句边界的定型
     * 证据，避免把 `Option<T-Fly>` 泄漏到后续 body 解析。
     */
    private fun collectProperDirectConstraintBounds(
        variableConstructor: TypeConstructorMarker,
    ): List<ConeCangJieType> {
        val storage = currentCommonSystem.currentStorage()
        val variableConstraints = storage.notFixedTypeVariables[variableConstructor]?.constraints.orEmpty()
        if (variableConstraints.isEmpty()) return emptyList()

        val substitutor = storage
            .buildCurrentSubstitutor(inferenceComponents.session.typeContext, emptyMap())
            .asCone()
        return variableConstraints
            .asSequence()
            .filter { constraint ->
                constraint.kind == ConstraintKind.UPPER || constraint.kind == ConstraintKind.EQUALITY
            }
            .mapNotNull { constraint ->
                val constraintType = constraint.type as? ConeCangJieType ?: return@mapNotNull null
                val substitutedType = substitutor.substituteOrNull(constraintType) ?: constraintType
                if (substitutedType is ConeErrorType || substitutedType.containsNotFixedVariable(storage)) {
                    null
                } else {
                    substitutedType
                }
            }
            .distinctBy { it.toString() }
            .toList()
    }

    /**
     * owner-sum 收敛到唯一候选时把它作为该形参变量的硬界。
     *
     * 官方在候选集归一到**单个类**（而非并集）后执行 `TryEnforceCandidate`，写 eq 约束
     * 把形参定死为该 owner（如 `{x => g29(x.v)}` 经 `FilterSumUpperbound` 淘汰 A29 后
     * 固定 `x := B29`）。CFIR 这一侧此前只把收敛结果写成 common system 的 subtype 约束，
     * 没有任何通路把它变成形参类型，导致体内成员访问始终停留在歧义状态。
     *
     * 仍含未定类型变量的表示（泛型 owner 的未替换轮，如 `B31<TypeVariable(T)>`）不作硬界，
     * 等替换轮给出具体形式后再定型；集合未收敛到单一 owner 时同样不提供硬界。
     */
    private fun convergedFreshReceiverOwnerBounds(
        variableConstructor: TypeConstructorMarker,
    ): List<ConeCangJieType> {
        val ownerType = freshReceiverCandidateOwnersByTypeVariable[variableConstructor]?.singleOrNull()
            ?: return emptyList()
        if (ownerType is ConeErrorType) return emptyList()
        if (ownerType.hasUndeterminedTypeVariableArgument()) return emptyList()
        return listOf(ownerType)
    }

    /**
     * 从语句边界排队调用的实参侧提取 owner-sum 过滤证据。
     *
     * 识别 argumentMapping 中「实参表达式自身是对目标形参变量的成员访问」的排队调用：
     * 实参是 qualified access 且其显式 receiver 正是形参 tyvar（如 g29(x.v) 的 x.v）。
     * 该实参的引用在多 owner 形状不等价时已退化为错误引用、不带原成员候选，因此这里
     * 用 receiver 的结构形态与引用名称识别，不依赖候选存活。此时以外层被调方该参数位
     * 的声明类型（按候选当前替换结果换算）为上界触发 [refineFreshReceiverOwnersByMemberReturnBound]；
     * 「调用自身就是对形参的成员访问」的排队调用不提供此类证据——那仍是 owner-sum 集合事实。
     */
    private fun refineOwnersFromOuterParameterBounds(
        variableConstructor: TypeConstructorMarker,
        atoms: List<ConeAtomWithCandidate>,
    ) {
        for (atom in atoms) {
            val candidate = atom.candidate
            if (candidate.isFreshReceiverMemberAccessOn(variableConstructor)) continue
            if (!candidate.argumentMappingInitialized) continue
            val substitutor = candidate.system.captureStorage()
                .buildCurrentSubstitutor(inferenceComponents.session.typeContext, emptyMap())
                .asCone()
            for ((argument, parameter) in candidate.argumentMapping) {
                refineOwnersFromOuterParameterBound(
                    variableConstructor,
                    argument.expression,
                    parameter,
                    substitutor,
                )
            }
        }
    }

    /**
     * 处理单个「外层调用实参位」提供的 owner 过滤证据。
     *
     * 逐项判定：实参必须是 qualified access、其显式 receiver 必须是目标形参 tyvar、
     * 成员名与被调形参声明界必须可读；任一不满足则该实参位不提供证据。
     */
    private fun refineOwnersFromOuterParameterBound(
        variableConstructor: TypeConstructorMarker,
        expression: CfirExpression,
        parameter: CfirValueParameter,
        substitutor: ConeSubstitutor,
    ) {
        val access = expression as? CfirQualifiedAccessExpression ?: return
        val receiverType = access.explicitReceiver?.coneTypeOrNull as? ConeTypeVariableType ?: return
        if (receiverType.typeConstructor != variableConstructor) return
        val memberName = (access.calleeReference as? CfirNamedReference)?.name ?: return
        val declaredBound = parameter.returnTypeRef.coneTypeOrNull ?: return
        val boundType = substitutor.substituteOrNull(declaredBound) ?: declaredBound
        refineFreshReceiverOwnersByMemberReturnBound(variableConstructor, memberName, boundType)
    }

    /**
     * 从语句边界内新增排队调用提取指定形参变量的硬界。
     *
     * 仅消费 argumentMapping（实参按候选自身替换结果换算到形参声明类型）；
     * 自身就是对该变量的成员访问的调用不提供硬界——那是 owner-sum 集合事实。
     */
    private fun collectHardBoundsForVariable(
        variableConstructor: TypeConstructorMarker,
        atoms: List<ConeAtomWithCandidate>,
    ): List<ConeCangJieType> {
        val bounds = mutableListOf<ConeCangJieType>()
        for (atom in atoms) {
            val candidate = atom.candidate
            if (candidate.isFreshReceiverMemberAccessOn(variableConstructor)) continue
            if (!candidate.argumentMappingInitialized) continue
            val substitutor = candidate.system.captureStorage()
                .buildCurrentSubstitutor(inferenceComponents.session.typeContext, emptyMap())
                .asCone()
            for ((argument, parameter) in candidate.argumentMapping) {
                val argumentType = argument.expression.coneTypeOrNull as? ConeTypeVariableType ?: continue
                if (argumentType.typeConstructor != variableConstructor) continue
                val parameterType = parameter.returnTypeRef.coneTypeOrNull?.let { declared ->
                    substitutor.substituteOrNull(declared) ?: declared
                } ?: continue
                if (parameterType is ConeErrorType) continue
                bounds += parameterType
            }
        }
        return bounds.distinctBy { it.toString() }
    }

    /**
     * 判断一组硬界是否存在唯一确定解。
     *
     * 解为全部硬界的交集；交集为 error、仍含未固定推断变量，或与 common system 中
     * 该变量的既有约束矛盾时返回 null（保守放弃，不制造新的错误路径）。
     */
    private fun solveUniqueSolution(
        variableConstructor: TypeConstructorMarker,
        hardBounds: List<ConeCangJieType>,
    ): ConeCangJieType? {
        val typeContext = inferenceComponents.session.typeContext
        val solution = org.cangnova.cangjie.cfir.types.ConeTypeIntersector.intersectTypes(typeContext, hardBounds)
        if (solution is ConeErrorType) return null
        val storage = currentCommonSystem.currentStorage()
        if (solution.containsNotFixedVariable(storage)) {
            return null
        }
        val substitutor = storage
            .buildCurrentSubstitutor(typeContext, emptyMap())
            .asCone()
        for (constraint in storage.notFixedTypeVariables[variableConstructor]?.constraints.orEmpty()) {
            val rawConstraintType = constraint.type as? ConeCangJieType ?: continue
            val constraintType = substitutor.substituteOrNull(rawConstraintType) ?: rawConstraintType
            if (constraintType is ConeErrorType) continue
            val compatible = when (constraint.kind) {
                ConstraintKind.UPPER -> AbstractTypeChecker.isSubtypeOf(typeContext, solution, constraintType)
                ConstraintKind.LOWER -> AbstractTypeChecker.isSubtypeOf(typeContext, constraintType, solution)
                ConstraintKind.EQUALITY -> AbstractTypeChecker.equalTypes(typeContext, solution, constraintType)

            }
            if (!compatible) return null
        }
        return solution
    }

    /** 判断类型是否仍引用当前 common system 中未固定的推断变量。 */
    private fun ConeCangJieType.containsNotFixedVariable(storage: ConstraintStorage): Boolean =
        contains { type ->
            type is ConeTypeVariableType && type.typeConstructor in storage.notFixedTypeVariables
        }

    /** 把 expected-type subtype 约束加入当前 common system。 */
    override fun addSubtypeConstraintIfCompatible(
        lowerType: ConeCangJieType,
        upperType: ConeCangJieType,
        element: CfirElement,
    ) {
        currentCommonSystem.addSubtypeConstraintIfCompatible(
            lowerType,
            upperType,
            ConeExpectedTypeConstraintPosition,
        )
    }

    /** 将模式构造器 equality 约束写入 PCLA common system。 */
    override fun addEqualityConstraintIfCompatible(
        firstType: ConeCangJieType,
        secondType: ConeCangJieType,
    ): Boolean {
        val accepted = currentCommonSystem.addEqualityConstraintIfCompatible(
            firstType,
            secondType,
            ConeExpectedTypeConstraintPosition,
        )
        return accepted
    }

    /**
     * 把重算后的末表达式类型约束到返回占位，返回该约束是否被接受。
     *
     * `false` 表示与共享系统既有约束冲突而被丢弃——这正是返回占位以
     * TypeVariable 泄漏给外层调用的直接来源。
     */
    fun constrainLambdaReturnPlaceholder(
        lowerType: ConeCangJieType,
        upperPlaceholder: ConeCangJieType,
    ): Boolean {
        return currentCommonSystem.addSubtypeConstraintIfCompatible(
            lowerType,
            upperPlaceholder,
            ConeExpectedTypeConstraintPosition,
        )
    }

    /** 把 PCLA body 内部新建的结构化推断变量纳入当前 common system。 */
    override fun registerInferenceVariable(variable: TypeVariableMarker) {
        val constructor = with(currentCommonSystem) { variable.freshTypeConstructor() }
        val previous = currentCommonSystem.allTypeVariables[constructor]
        if (previous == null) currentCommonSystem.registerVariable(variable)
        else check(previous === variable) { "A retained constructor was rebound to another inference variable" }
        /*
         * ConstraintSystemCompleter 收集需要固定的变量时，除了候选自身的 fresh
         * variables，还会读取 additionalCompletionVariables。模式推断创建的
         * Option<T> 元素变量不属于 callable 声明参数，若只登记到 common system，
         * 它们会留在约束表中却永远不进入 fixation 队列。
         */
        (variable as? ConeTypeVariable)?.let { typeVariable ->
            outerCandidate.additionalCompletionVariables += typeVariable.typeConstructor
        }
    }

    /** 判断类型中是否含当前 PCLA 公共系统已登记的推断变量。 */
    override fun containsRegisteredInferenceVariable(type: ConeCangJieType): Boolean =
        type.contains { it is ConeTypeVariableType && it.typeConstructor in currentCommonSystem.currentStorage().allTypeVariables }

    /**
     * 按官方 `SynLamExpr` 的语法候选收窄规则维护 fresh receiver owner 集合。
     *
     * 官方 `TryInitializeBaseSum` **只在 sum 还是 {Any} 时播种**，之后每次成员访问只在
     * 既有 sum 上做过滤。因此首次访问取本次候选集，后续访问一律取交集；交集为空说明本次
     * 候选与既有 sum 矛盾（如 `{x => x.foo34(); x + x}` 中 `x + x` 只推导出声明者 A36，
     * 与 foo34 播下的 {Unit, Int64} 无交集），此时**保留既有集合**——绝不能采纳矛盾的新
     * 集合，否则单元素的新集合会被当成收敛结果写死错误的形参约束（`x <: A36`）。
     *
     * 泛型 owner 会经历「未替换类型实参」与「替换后」两轮解析（body 级重转通道），
     * 两轮交错共享本集合：严格 equalTypes 会把 `B31<TypeVariable(T)>` 与 `B31<Int64>`
     * 判为不同导致交集永远为空、集合无法收敛，因此跨轮比较按
     * [ConeCangJieType.isSameFreshReceiverOwnerCandidate] 的构造器近似语义执行，
     * 幸存时保留更具体一侧的表示。
     */
    override fun refineFreshReceiverCandidateOwners(
        receiverTypeConstructor: TypeConstructorMarker,
        ownerTypes: List<ConeCangJieType>,
    ): Set<ConeCangJieType>? {
        val distinctOwnerTypes = ownerTypes.distinctByConeType()
        if (distinctOwnerTypes.isEmpty()) return null

        val previous = freshReceiverCandidateOwnersByTypeVariable[receiverTypeConstructor]
        val refined = when (previous) {
            null -> distinctOwnerTypes
            else -> buildList {
                for (ownerType in distinctOwnerTypes) {
                    val matchedPrevious = previous.firstOrNull { previousOwner ->
                        previousOwner.isSameFreshReceiverOwnerCandidate(ownerType)
                    } ?: continue
                    add(matchedPrevious.mergeSameOwnerRepresentation(ownerType))
                }
            }.ifEmpty { previous }
        }

        freshReceiverCandidateOwnersByTypeVariable[receiverTypeConstructor] = refined.toMutableList()
        addFreshReceiverOwnerConstraintIfSingle(receiverTypeConstructor, refined)
        return refined.toSet()
    }

    /**
     * 判断两个 owner 类型是否指向同一候选声明。
     *
     * 严格相等优先；仅一侧仍含未定类型变量实参时（未替换轮 vs 替换轮），退化为按
     * 顶层 lookup tag 近似相等——同一泛型声明的两轮表示指同一个候选；两侧都已具体化
     * 则必须严格相等，避免把同类不同实参的真实差异合并掉。
     */
    private fun ConeCangJieType.isSameFreshReceiverOwnerCandidate(other: ConeCangJieType): Boolean {
        if (isSameConeType(other)) return true
        if (!hasUndeterminedTypeVariableArgument() && !other.hasUndeterminedTypeVariableArgument()) return false
        val thisTag = (this as? ConeClassifierType)?.lookupTag ?: return false
        val otherTag = (other as? ConeClassifierType)?.lookupTag ?: return false
        return thisTag == otherTag
    }

    /**
     * 合并同一 owner 候选的两轮表示，优先保留不含未定类型变量的具体形式；
     * 双方都未具体化时取新一轮表示即可，二者近似等价。
     */
    private fun ConeCangJieType.mergeSameOwnerRepresentation(other: ConeCangJieType): ConeCangJieType =
        if (hasUndeterminedTypeVariableArgument()) other else this

    /** 类型自身或其类型实参中是否仍含未定类型变量（替换前轮次的标志）。 */
    private fun ConeCangJieType.hasUndeterminedTypeVariableArgument(): Boolean =
        contains { it is ConeTypeVariableType }

    /** 记录 resolver 在成员访问解析时暴露的各 owner 成员返回类型形状。 */
    override fun recordFreshReceiverMemberAccessShapes(
        receiverTypeConstructor: TypeConstructorMarker,
        shapes: List<FreshReceiverMemberAccessShape>,
    ) {
        val existing = freshReceiverMemberShapesByTypeVariable.getOrPut(receiverTypeConstructor) { mutableListOf() }
        for (shape in shapes) {
            if (existing.none { it == shape }) existing += shape
        }
    }

    /** 返回该 fresh receiver 当前维护的 owner 候选集合副本。 */
    override fun knownCandidateOwnerTypesForFreshReceiver(
        receiverTypeConstructor: TypeConstructorMarker,
    ): List<ConeCangJieType> =
        freshReceiverCandidateOwnersByTypeVariable[receiverTypeConstructor].orEmpty().toList()

    /**
     * 用外部被调方形参类型作目标过滤 owner 集并并入交集语义（官方 FilterSumUpperbound）。
     *
     * 仅当存在同名成员形状记录时执行：逐 owner 检查该成员返回类型是否为上界子类型，
     * 全部淘汰说明上界与既有集合矛盾，保守维持现状交由普通诊断报告；无新增信息
     * （幸存者不变少）时也直接跳过。幸存集合经 [refineFreshReceiverCandidateOwners]
     * 与既有集合作交，单一收敛时写入真正的形参约束。
     */
    override fun refineFreshReceiverOwnersByMemberReturnBound(
        receiverTypeConstructor: TypeConstructorMarker,
        memberName: Name,
        returnTypeUpperBound: ConeCangJieType,
    ) {
        if (returnTypeUpperBound is ConeErrorType) return
        val currentOwners = freshReceiverCandidateOwnersByTypeVariable[receiverTypeConstructor] ?: return
        val shapes = freshReceiverMemberShapesByTypeVariable[receiverTypeConstructor]
            ?.filter { it.memberName == memberName }
            ?.takeIf { it.isNotEmpty() }
            ?: return
        val typeContext = inferenceComponents.session.typeContext
        val survivors = currentOwners.filter { owner ->
            shapes.any { shape ->
                shape.ownerType.isSameConeType(owner) &&
                        AbstractTypeChecker.isSubtypeOf(typeContext, shape.returnType, returnTypeUpperBound)
            }
        }
        // 全部淘汰说明上界与既有集合矛盾，保守维持现状；幸存者不变少则没有新信息。
        if (survivors.isEmpty() || survivors.size == currentOwners.size) return
        refineFreshReceiverCandidateOwners(receiverTypeConstructor, survivors)
    }

    /**
     * owner 候选集合收敛到单一类型时，把它转成真正的 PCLA 约束。
     *
     * 官方 `TryEnforceCandidate` 会在能确定唯一 owner 构造器时约束 placeholder；
     * CFIR 的在线成员候选规约在多候选阶段只维护 owner-sum，直到交集变为单一候选再
     * 写入 common system，避免第一条成员访问过早把等价候选中的代表类型固定下来。
     */
    private fun addFreshReceiverOwnerConstraintIfSingle(
        receiverTypeConstructor: TypeConstructorMarker,
        ownerTypes: List<ConeCangJieType>,
    ) {
        val ownerType = ownerTypes.singleOrNull() ?: return
        // 候选的 fresh 变量只随被选中的候选系统进入 PCLA，不能从 owner 集合另造变量。
        // 泛型形状由成员候选自身的 receiver 约束保存；这里只提交已确定的 owner 类型。
        if (ownerType.hasUndeterminedTypeVariableArgument()) return
        val receiverType = (currentCommonSystem.currentStorage().allTypeVariables[receiverTypeConstructor] as? ConeTypeVariable)
            ?.defaultType
            ?: return
        currentCommonSystem.addSubtypeConstraintIfCompatible(
            receiverType,
            ownerType,
            ConeExpectedTypeConstraintPosition,
        )
    }

    /** 按当前 session 类型等价关系去重，避免类型实参结构相同但对象不同导致集合无法相交。 */
    private fun List<ConeCangJieType>.distinctByConeType(): List<ConeCangJieType> {
        val result = mutableListOf<ConeCangJieType>()
        for (type in this) {
            if (result.none { existing -> existing.isSameConeType(type) }) {
                result += type
            }
        }
        return result
    }

    /** 判断两个 Cone 类型是否为同一个 owner 候选。 */
    private fun ConeCangJieType.isSameConeType(other: ConeCangJieType): Boolean =
        AbstractTypeChecker.equalTypes(inferenceComponents.session.typeContext, this, other)

    /** 使用当前约束系统 substitutor 更新表达式返回类型。 */
    private fun CfirExpression.updateReturnTypeWithCurrentSubstitutor(
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
    ) {
        val system = (this as? CfirResolvable)?.candidate()?.system ?: currentCommonSystem
        val currentType = coneTypeOrNull ?: return
        val additionalBinding = if (resolutionMode is org.cangnova.cangjie.cfir.resolve.ResolutionMode.ReceiverResolution) {
            semiFixCurrentResultIfTypeVariableAndReturnBinding(currentType, system)
        } else null
        // 接收者只使用本次半固定结果；完整 semiFixedVariables 会改变 PCLA 诊断中的类型。
        val additionalBindings: Map<TypeConstructorMarker, org.cangnova.cangjie.type.model.CangJieTypeMarker> =
            additionalBinding?.let {
                mapOf<TypeConstructorMarker, org.cangnova.cangjie.type.model.CangJieTypeMarker>(it)
            }.orEmpty()
        val substitutor = system.buildCurrentSubstitutor(additionalBindings).asCone()
        val updatedType = substitutor.substituteOrNull(currentType) ?: return
        replaceConeTypeOrNull(updatedType)
    }

    /** 从可解析表达式上读取当前候选。 */
    private fun CfirResolvable.candidate(): Candidate? =
        (calleeReference as? CfirNamedReferenceWithCandidate)?.candidate

    /** 判断候选是否可以脱离 PCLA common system 独立分析并完成。 */
    private fun Candidate.mightBeAnalyzedAndCompletedIndependently(bodyResolveContext: BodyResolveContext): Boolean {
        when (val mode = callInfo.resolutionMode) {
            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.WithExpectedType -> {
                if (mode.expectedType.containsNotFixedTypeVariables()) return false
            }

            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.WithStatus,
            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.UpdateImplicitTypeRef,
            -> error("$this call should not be analyzed in ${callInfo.resolutionMode}")

            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.ContextDependent,
            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.ContextIndependent,
            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.ReceiverResolution,
            -> {
            }
        }

        val callSite = callInfo.callSite
        if (callSite is CfirAnnotationCall) return true
        if (callSite is CfirArrayLiteral) return bodyResolveContext.isInsideAnnotationContext
        if (callSite !is CfirResolvable) return false

        if (dispatchReceiver?.expression?.isReceiverPostponed() == true) return false
        if (givenExtensionReceiver?.expression?.isReceiverPostponed() == true) return false

        val returnType = (symbol as? CfirCallableSymbol<*>)?.cfir
            ?.let { it as? CfirCallableDeclaration }
            ?.let(bodyResolveContext.returnTypeCalculator::tryCalculateReturnType)
        if (returnType?.coneType?.containsNotFixedTypeVariables() == true) return false

        if (callInfo.arguments.any { !it.isTrivialArgument() }) return false
        return true
    }

    /** 判断表达式是否是不会向 PCLA common system 注入新约束的简单实参。 */
    private fun CfirExpression.isTrivialArgument(): Boolean = when (this) {
        is CfirArrayLiteral -> false
        is CfirResolvable -> {
            // 多 owner 成员的错误结果仍依赖真实 receiver，外层参数约束必须进入共享推断。
            if ((this as? CfirQualifiedAccessExpression)?.explicitReceiver?.isReceiverPostponed() == true) false
            else when (val candidate = candidate()) {
                null -> coneTypeOrNull?.containsNotFixedTypeVariables() != true
                else -> !candidate.usedOuterCs
            }
        }

        is CfirWrappedExpression -> expression.isTrivialArgument()
        is CfirFunctionCall -> argumentList.arguments.all { it.isTrivialArgument() }
        is CfirBinaryOp -> left.isTrivialArgument() && right.isTrivialArgument()
        is CfirComparisonExpression -> left.isTrivialArgument() && right.isTrivialArgument()
        is CfirBlock -> (statements.lastOrNull() as? CfirExpression)?.isTrivialArgument() ?: true
        is CfirTupleLiteral -> elements.all { it.isTrivialArgument() }
        is CfirStringInterpolation -> parts.all { it.isTrivialArgument() }
        is CfirLiteralExpression -> true
        else -> false
    }

    /** 判断接收者表达式是否仍依赖 postponed 或未固定类型变量。 */
    private fun CfirExpression.isReceiverPostponed(): Boolean {
        return when {
            coneTypeOrNull?.containsNotFixedTypeVariables() == true -> true
            (this as? CfirResolvable)?.candidate()?.usedOuterCs == true -> true
            else -> false
        }
    }

    /** 判断类型中是否包含当前 common system 尚未固定的类型变量。 */
    private fun ConeCangJieType.containsNotFixedTypeVariables(): Boolean =
        contains {
            it is ConeTypeVariableType && it.typeConstructor in currentCommonSystem.allTypeVariables
        }
}

/**
 * PCLA 完成后替换 lambda 子树中遗留的类型变量。
 *
 * 无上下文 lambda 会先用 placeholder 解析 body，再由外层调用完成约束。完成阶段必须
 * 把最终 substitutor 应用到已分析 lambda 的表达式类型、resolved type ref 和候选 receiver
 * atom；否则后续 checker 仍会看到旧的 placeholder 类型并产生级联诊断。
 */
@OptIn(CfirAbstractResolveTransformerSubclassing::class)
class CfirTypeVariablesAfterPCLATransformer(
    private val substitutor: ConeSubstitutor,
) : CfirAbstractResolveTransformer<Nothing?>() {

    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirResolvable) {
            element.calleeReference.candidateOrNull()?.let(::processCandidate)
        }

        if (element is CfirExpression && element.canReplaceTypeAfterPCLA()) {
            element.coneTypeOrNull
                ?.let(substitutor::substituteOrNull)
                ?.let(element::replaceConeTypeOrNull)
        }

        @Suppress("UNCHECKED_CAST")
        return element.transformChildren(this, null) as E
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: Nothing?): CfirTypeRef {
        return substitutor.substituteOrNull(resolvedTypeRef.coneType)
            ?.let { resolvedTypeRef.resolvedTypeFromPrototype(it, resolvedTypeRef.source) }
            ?: resolvedTypeRef
    }

    /**
     * candidate receiver atom 不是普通 children；PCLA 后要显式同步其中表达式的最终类型。
     */
    private fun processCandidate(candidate: Candidate) {
        candidate.dispatchReceiver = candidate.dispatchReceiver.transformAtomExpression()
        candidate.chosenExtensionReceiver = candidate.chosenExtensionReceiver.transformAtomExpression()
        candidate.contextArguments = candidate.contextArguments?.mapNotNull { atom ->
            atom.transformAtomExpression()
        }
    }

    private fun ConeResolutionAtom?.transformAtomExpression(): ConeResolutionAtom? =
        ConeResolutionAtom.createRawAtom(this?.expression?.transform(this@CfirTypeVariablesAfterPCLATransformer, null))

    private fun CfirReference.candidateOrNull(): Candidate? =
        (this as? CfirNamedReferenceWithCandidate)?.candidate

    private fun CfirExpression.canReplaceTypeAfterPCLA(): Boolean =
        this !is CfirAnonymousFunctionExpression &&
                this !is CfirErrorExpression &&
                this !is CfirLazyBlock &&
                this !is CfirLazyExpression
}
