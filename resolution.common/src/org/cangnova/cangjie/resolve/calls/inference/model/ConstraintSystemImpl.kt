/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.*
import org.cangnova.cangjie.resolve.calls.inference.components.*
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeInfo
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.utils.SmartList
import org.cangnova.cangjie.utils.SmartSet
import org.cangnova.cangjie.utils.trimToSize
import kotlin.math.max
import kotlin.reflect.KFunction

/**
 * 约束系统的默认可变实现。
 *
 * 该实现同时承担约束构建、推断完成、postponed 参数分析和约束注入上下文职责，内部通过
 * [MutableConstraintStorage] 保存类型变量、约束、错误、fork point 和缓存状态。
 */
class ConstraintSystemImpl(
    /**
     * 负责注入初始约束并驱动 incorporation 的约束注入器。
     */
    private val constraintInjector: ConstraintInjector,

    /**
     * 当前类型系统推断上下文。
     */
    val typeSystemContext: TypeSystemInferenceExtensionContext,

    /**
     * 当前语言版本设置。
     */
    private val languageVersionSettings: LanguageVersionSettings,
) : ConstraintSystemCompletionContext(),
    TypeSystemInferenceExtensionContext by typeSystemContext,
    ConstraintSystem,
    ConstraintSystemBuilder,
    ConstraintSystemMarker,
    ConstraintInjector.Context,
    ResultTypeResolver.Context,
    PostponedArgumentsAnalyzerContext {
    /**
     * incorporation 与补全阶段共享的约束系统工具上下文。
     */
    private val utilContext = constraintInjector.constraintIncorporator.utilContext

    /**
     * 当前约束系统使用的推断日志器。
     */
    private val inferenceLogger = constraintInjector.constraintIncorporator.inferenceLogger

    /**
     * 等全部类型变量固定后延迟执行的检查或计算。
     */
    private val postponedComputationsAfterAllVariablesAreFixed = mutableListOf<() -> Unit>()

    /**
     * 当前约束系统的可变存储。
     */
    private val storage = MutableConstraintStorage().also { it.owner = this }
    private var sourceMutationDepth = 0
    private var constraintGenerationDepth = 0
    private var parentRebaseSuspendedDepth = 0

    /** actual common-system 暂借此 CS 时，getter 不得恢复旧 parent 视图。 */
    fun <R> withParentRebaseSuspended(block: () -> R): R {
        parentRebaseSuspendedDepth++
        return try { block() } finally { parentRebaseSuspendedDepth-- }
    }

    /** 下一次语义操作前同步；事务和实际 analyzer 写入期间保持当前 CS。 */
    fun synchronizeWithOwner(owner: ConstraintSystemImpl = checkNotNull(storage.parentBaseline).owner) {
        // Kotlin asReadOnlyStorage 的 FREEZED 是终止可变写入的边界，保留已发布 storage。
        if (state == State.FREEZED || parentRebaseSuspendedDepth != 0 || sourceMutationDepth != 0 || state == State.TRANSACTION) return
        if (owner === this) return
        val baseline = checkNotNull(storage.parentBaseline)
        // 所有权同步只读取实际 parent 的当前事实；currentStorage 是受状态保护的可变上下文入口。
        if (!sameBaselineState(baseline.storage, owner.storage)) rebaseToOwner(owner)
    }

    private fun sameBaselineState(a: ConstraintStorage, b: ConstraintStorage): Boolean {
        val first = checkNotNull(provenance(a))
        val second = checkNotNull(provenance(b))
        return a.allTypeVariables.keys.toList() == b.allTypeVariables.keys.toList() &&
            a.allTypeVariables.all { (key, variable) -> b.allTypeVariables[key] === variable } &&
            a.fixedTypeVariables == b.fixedTypeVariables &&
            a.notFixedTypeVariables.keys == b.notFixedTypeVariables.keys &&
            first.notFixedTypeVariables.all { (key, row) ->
                val other = second.notFixedTypeVariables.getValue(key)
                val raw = row.rawConstraintsSnapshot()
                val otherRaw = other.rawConstraintsSnapshot()
                raw.size == otherRaw.size && raw.zip(otherRaw).all { (x, y) -> x === y }
            } && a.initialConstraints == b.initialConstraints && a.errors == b.errors &&
            a.postponedTypeVariables == b.postponedTypeVariables &&
            a.constraintsFromAllForkPoints.size == b.constraintsFromAllForkPoints.size &&
            a.constraintsFromAllForkPoints.zip(b.constraintsFromAllForkPoints).all { (x, y) -> x === y } &&
            first.resolvedForks == second.resolvedForks && first.mutations == second.mutations &&
            first.unrestrictedBuilderInference == second.unrestrictedBuilderInference &&
            a.typeVariableDependencies == b.typeVariableDependencies &&
            a.maxTypeDepthFromInitialConstraints == b.maxTypeDepthFromInitialConstraints &&
            a.outerSystemVariablesPrefixSize == b.outerSystemVariablesPrefixSize && a.usesOuterCs == b.usesOuterCs &&
            a.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables == b.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables &&
            a.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables == b.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables
    }

    private fun <T> isOrderedSubsequence(before: List<T>, after: List<T>): Boolean {
        var next = 0
        for (element in after) if (next < before.size && before[next] === element) next++
        return next == before.size
    }

    private inline fun generatingConstraints(block: () -> Unit) {
        constraintGenerationDepth++
        try { block() } finally { constraintGenerationDepth-- }
    }

    private fun newMutation(
        completion: Boolean,
        removal: Boolean = false,
        fixedVariable: TypeVariableMarker? = null,
        action: (ConstraintSystemImpl) -> Unit,
    ): ConstraintSystemMutation = ConstraintSystemMutation(
        completion, removal, fixedVariable, typeVariablesThatAreCountedAsProperTypes?.toSet(),
        allowSemiFixationToOtherTypeVariables, action,
    )

    /** 每次实际源操作有一个稳定对象身份；其派生写入不再重复登记。 */
    private fun sourceMutation(
        removal: Boolean = false,
        fixedVariable: TypeVariableMarker? = null,
        action: (ConstraintSystemImpl) -> Unit,
        block: () -> Unit,
    ) {
        if (sourceMutationDepth == 0) storage.mutations += newMutation(atCompletionState, removal, fixedVariable, action)
        sourceMutationDepth++
        try { block() } finally { sourceMutationDepth-- }
    }

    /** 在语义操作边界复制全部 raw row、队列及来源，保留每个 VM 和 source constraint 身份。 */
    fun captureStorage(): ConstraintSystemSnapshot = ConstraintSystemSnapshot(this, copyStorage(storage))
    /** 基线绑定实际可变 owner；asOuter 仅表达 Kotlin 的外层变量前缀合同。 */
    fun captureBaseline(asOuter: Boolean = false): ConstraintSystemBaseline = ConstraintSystemBaseline(this, captureStorage(), asOuter)
    val parentConstraintSystemOwner: ConstraintSystemImpl? get() = storage.parentBaseline?.owner

    /** 与 Kotlin setBaseSystem/addOuterSystem 相同克隆边界，保留真实 source CS 基线。 */
    fun fork(asOuter: Boolean = false): ConstraintSystemImpl =
        ConstraintSystemImpl(constraintInjector, typeSystemContext, languageVersionSettings).also {
            if (asOuter) it.addOuterSystem(captureStorage()) else it.setBaseSystem(captureStorage())
        }

    private fun provenance(from: ConstraintStorage): MutableConstraintStorage? = when (from) {
        is MutableConstraintStorage -> from
        is ConstraintSystemSnapshot -> from.content
        else -> null
    }

    private fun baselineContainsOwner(baseline: ConstraintSystemBaseline?, owner: ConstraintSystemImpl): Boolean =
        baseline != null && baselineChain(baseline).any { it.owner === owner }

    /** 实际当前基线与历次捕获的 source 构成快照 DAG；共享节点不是 source cycle。 */
    private fun baselineChain(baseline: ConstraintSystemBaseline): List<ConstraintSystemBaseline> {
        val chain = mutableListOf<ConstraintSystemBaseline>()
        val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<ConstraintSystemBaseline, Boolean>())
        val active = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<ConstraintSystemBaseline, Boolean>())
        val pending = java.util.ArrayDeque<Pair<ConstraintSystemBaseline, Boolean>>()
        pending.addLast(baseline to false)
        while (pending.isNotEmpty()) {
            val (current, leaving) = pending.removeLast()
            if (leaving) {
                active.remove(current)
                continue
            }
            check(current !in active) { "A constraint-system baseline contains a source cycle" }
            if (!visited.add(current)) continue
            active += current
            chain += current
            pending.addLast(current to true)
            current.retainedSources.asReversed().forEach { pending.addLast(it to false) }
            provenance(current.storage)?.parentBaseline?.let { pending.addLast(it to false) }
        }
        return chain
    }

    /** 沿真实派生基线检查当时仍活动的变量；父后来同值固定不能抹掉 child 越权事实。 */
    private fun baselineHasUnfixedVariable(
        baseline: ConstraintSystemBaseline?,
        owner: ConstraintSystemImpl,
        key: TypeConstructorMarker,
    ): Boolean {
        return baseline != null && baselineChain(baseline).any {
            it.owner === owner && key in it.storage.notFixedTypeVariables
        }
    }

    private fun copyStorage(from: ConstraintStorage): MutableConstraintStorage = MutableConstraintStorage().also { copy ->
        copy.owner = provenance(from)?.owner ?: this
        copy.allTypeVariables.putAll(from.allTypeVariables)
        from.notFixedTypeVariables.forEach { (key, row) ->
            copy.notFixedTypeVariables[key] = if (row is MutableVariableWithConstraints) row.snapshot(this) else MutableVariableWithConstraints(this, row)
        }
        from.typeVariableDependencies.forEach { (key, dependencies) -> copy.typeVariableDependencies[key] = dependencies.toMutableSet() }
        copy.initialConstraints.addAll(from.initialConstraints)
        copy.fixedTypeVariables.putAll(from.fixedTypeVariables)
        copy.errors.addAll(from.errors)
        copy.maxTypeDepthFromInitialConstraints = from.maxTypeDepthFromInitialConstraints
        copy.postponedTypeVariables.addAll(from.postponedTypeVariables)
        copy.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables.putAll(from.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables)
        copy.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables.putAll(from.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables)
        copy.constraintsFromAllForkPoints.addAll(from.constraintsFromAllForkPoints)
        copy.outerSystemVariablesPrefixSize = from.outerSystemVariablesPrefixSize
        copy.usesOuterCs = from.usesOuterCs
        provenance(from)?.let { origin ->
            copy.unrestrictedBuilderInference = origin.unrestrictedBuilderInference
            copy.parentBaseline = origin.parentBaseline
            copy.mutations.addAll(origin.mutations)
            copy.resolvedForks.addAll(origin.resolvedForks)
            @OptIn(AssertionsOnly::class)
            run { copy.outerCS = origin.outerCS }
        }
        from.approximatorCaches.forEach { (configuration, cache) ->
            copy.approximatorCaches[configuration] = AbstractTypeApproximator.Cache().also { it += cache }
        }
    }

    /** 同一事务快照或已验证的派生结果一次安装；不把旧 notFixed 行做并集。 */
    private fun installStorage(from: ConstraintStorage, preserveRows: Boolean = true) {
        val previousRows = if (preserveRows) storage.notFixedTypeVariables.toMap() else emptyMap()
        val copy = copyStorage(from)
        storage.allTypeVariables.clear()
        storage.notFixedTypeVariables.clear()
        storage.typeVariableDependencies.clear()
        storage.initialConstraints.clear()
        storage.fixedTypeVariables.clear()
        storage.errors.clear()
        storage.postponedTypeVariables.clear()
        storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables.clear()
        storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables.clear()
        storage.constraintsFromAllForkPoints.clear()
        storage.approximatorCaches.clear()
        storage.mutations.clear()
        storage.resolvedForks.clear()
        storage.unrestrictedBuilderInference = copy.unrestrictedBuilderInference
        storage.parentBaseline = copy.parentBaseline
        storage.usesOuterCs = copy.usesOuterCs
        storage.outerSystemVariablesPrefixSize = copy.outerSystemVariablesPrefixSize
        @OptIn(AssertionsOnly::class)
        run { storage.outerCS = copy.outerCS }
        doAddOtherSystem(copy, mergeMode = false)
        // 对齐 Kotlin 的事务原地恢复：incorporation 可持有已存在 row 的对象引用。
        for ((key, saved) in copy.notFixedTypeVariables) {
            previousRows[key]?.let { original ->
                original.restoreConstraintsFrom(saved)
                storage.notFixedTypeVariables[key] = original
            }
        }
        storage.maxTypeDepthFromInitialConstraints = copy.maxTypeDepthFromInitialConstraints
        properTypesCache.clear()
        notProperTypesCache.clear()
        intersectionTypesCache.clear()
        hasContradictionInForkPointsCache = null
    }

    private fun checkPartition(from: ConstraintStorage) {
        check(from.fixedTypeVariables.keys.none { it in from.notFixedTypeVariables })
        check(from.allTypeVariables.keys == from.fixedTypeVariables.keys + from.notFixedTypeVariables.keys)
        from.notFixedTypeVariables.forEach { (key, row) -> check(from.allTypeVariables[key] === row.typeVariable) }
    }

    private fun replayMutation(mutation: ConstraintSystemMutation) {
        val previousState = state
        val previousCompletion = atCompletionState
        val previousProperVariables = typeVariablesThatAreCountedAsProperTypes
        val previousAllowSemiFixation = allowSemiFixationToOtherTypeVariables
        typeVariablesThatAreCountedAsProperTypes = mutation.properVariables
        allowSemiFixationToOtherTypeVariables = mutation.allowSemiFixation
        properTypesCache.clear()
        notProperTypesCache.clear()
        state = if (mutation.completion) State.COMPLETION else State.BUILDING
        atCompletionState = mutation.completion
        sourceMutationDepth++
        try {
            mutation.action(this)
            if (storage.mutations.none { it === mutation }) storage.mutations += mutation
        } finally {
            sourceMutationDepth--
            state = previousState
            atCompletionState = previousCompletion
            typeVariablesThatAreCountedAsProperTypes = previousProperVariables
            allowSemiFixationToOtherTypeVariables = previousAllowSemiFixation
            properTypesCache.clear()
            notProperTypesCache.clear()
        }
    }

    /** 真实基线和源操作 delta 在实际 owner 的当前状态上重放，derived rows 仍由 injector 生成。 */
    private fun rebaseStorage(
        incoming: ConstraintStorage,
        baseline: ConstraintSystemBaseline,
        protectParent: Boolean,
    ): ConstraintSystemSnapshot {
        checkPartition(incoming)
        check(baseline.owner === this || baselineContainsOwner(provenance(baseline.storage)?.parentBaseline, this) ||
            baselineContainsOwner(storage.parentBaseline, baseline.owner)) {
            "The receiving CS is not the actual source owner or its transferred descendant"
        }
        val parent = captureStorage()
        checkPartition(parent)
        val sourceChain = baselineChain(baseline)
        val receiverBaseline = sourceChain.firstOrNull { it.owner === this }
        if (receiverBaseline == null) check(baselineContainsOwner(storage.parentBaseline, baseline.owner))
        val base = (receiverBaseline ?: baseline).storage
        // 选择 receiver 的同步起点不撤销中间 source；所有捕获过的 VM 都必须仍在 incoming 中。
        sourceChain.forEach { source ->
            source.storage.allTypeVariables.forEach { (key, variable) ->
                check(incoming.allTypeVariables[key] === variable) { "A branch changed its captured source variable identity" }
            }
        }
        base.allTypeVariables.forEach { (key, variable) ->
            check(parent.allTypeVariables[key] === variable && incoming.allTypeVariables[key] === variable) {
                "A derived branch lost its actual parent variable identity: " +
                    "constructor=${System.identityHashCode(key)}, baselineVM=${System.identityHashCode(variable)}, " +
                    "receiverVM=${parent.allTypeVariables[key]?.let(System::identityHashCode)}, " +
                    "incomingVM=${incoming.allTypeVariables[key]?.let(System::identityHashCode)}, " +
                    "baselineOwner=${System.identityHashCode(baseline.owner)}, receiver=${System.identityHashCode(this)}, " +
                    "receiverParent=${storage.parentBaseline?.owner?.let(System::identityHashCode)}, " +
                    "baselineOwnerIsReceiver=${baseline.owner === this}, " +
                    "receiverIsBaselineAncestor=${baselineContainsOwner(provenance(base)?.parentBaseline, this)}, " +
                    "receiverIsBaselineDescendant=${baselineContainsOwner(storage.parentBaseline, baseline.owner)}, " +
                    "variables=${base.allTypeVariables.size}/${parent.allTypeVariables.size}/${incoming.allTypeVariables.size}"
            }
        }
        val before = checkNotNull(provenance(base)).mutations
        val incomingHistory = checkNotNull(provenance(incoming)).mutations
        sourceChain.forEach { source ->
            val sourceHistory = checkNotNull(provenance(source.storage)).mutations
            check(isOrderedSubsequence(sourceHistory, incomingHistory)) { "A branch crossed a reverted source transaction" }
            check(isOrderedSubsequence(sourceHistory, checkNotNull(provenance(source.owner.storage)).mutations)) {
                "A source owner reverted this branch's baseline"
            }
        }
        check(isOrderedSubsequence(before, storage.mutations)) { "The owner reverted this branch's baseline" }
        val result = ConstraintSystemImpl(constraintInjector, typeSystemContext, languageVersionSettings)
        result.installStorage(parent)
        for (mutation in incomingHistory) {
            if (before.any { it === mutation } || result.storage.mutations.any { it === mutation }) continue
            mutation.fixedVariable?.let { variable ->
                val constructor = variable.freshTypeConstructor()
                check(!protectParent || constructor !in parent.notFixedTypeVariables &&
                    sourceChain.none { source ->
                        constructor in source.storage.notFixedTypeVariables &&
                            checkNotNull(provenance(source.storage)).mutations.none { it === mutation } &&
                            // 同一真实 source 后来合法固定自己的 VM，可由其捕获快照中的操作身份证明。
                            sourceChain.none { observed ->
                                observed.owner === source.owner &&
                                    checkNotNull(provenance(observed.storage)).mutations.any { it === mutation }
                            }
                    }) {
                    "Private inference attempted to fix an active parent variable"
                }
            }
            result.replayMutation(mutation)
        }
        // 删除携带当时实际被移除的 InitialConstraint 身份，只退役同一来源的派生 row。
        result.storage.mutations.filter { it.removal }.toList().forEach(result::replayMutation)
        val substitutor = result.buildCurrentSubstitutor()
        base.fixedTypeVariables.forEach { (key, type) ->
            check(substitutor.safeSubstitute(checkNotNull(incoming.fixedTypeVariables[key])) == substitutor.safeSubstitute(type))
            check(substitutor.safeSubstitute(checkNotNull(parent.fixedTypeVariables[key])) == substitutor.safeSubstitute(type))
        }
        sourceChain.forEach { source ->
            source.storage.fixedTypeVariables.forEach { (key, type) ->
                check(substitutor.safeSubstitute(checkNotNull(incoming.fixedTypeVariables[key])) == substitutor.safeSubstitute(type))
                check(substitutor.safeSubstitute(checkNotNull(result.fixedTypeVariables[key])) == substitutor.safeSubstitute(type)) {
                    "A source baseline fixed binding disagrees with ancestor replay"
                }
            }
        }
        // incoming 的公开 fixed map 也属于输入合同，不能让 journal 重放隐藏不同绑定。
        incoming.allTypeVariables.forEach { (key, variable) ->
            check(result.allTypeVariables[key] === variable) { "A derived storage lost its incoming VM" }
        }
        incoming.fixedTypeVariables.forEach { (key, type) ->
            check(substitutor.safeSubstitute(checkNotNull(result.fixedTypeVariables[key])) ==
                substitutor.safeSubstitute(type)) { "Incoming fixed binding disagrees with its source replay" }
        }
        checkPartition(result.storage)
        result.storage.parentBaseline = storage.parentBaseline
        return result.captureStorage()
    }

    /** getter 不以 keys 相同判定同步；captured base 只决定 delta 起点。 */
    fun rebaseToOwner(owner: ConstraintSystemImpl = checkNotNull(storage.parentBaseline).owner) {
        checkState(State.BUILDING, State.COMPLETION)
        check(parentRebaseSuspendedDepth == 0 && sourceMutationDepth == 0 && state != State.TRANSACTION)
        val baseline = checkNotNull(storage.parentBaseline)
        check(owner !== this)
        val incoming = captureStorage()
        val sourceChain = baselineChain(baseline)
        val rebased = owner.rebaseStorage(incoming, baseline, protectParent = true)
        val outerVariables = if (incoming.outerSystemVariablesPrefixSize == 0) emptySet() else {
            val inherited = incoming.allTypeVariables.keys.take(incoming.outerSystemVariablesPrefixSize)
            val ownerIsOuter = baseline.asOuter || sourceChain.any { it.owner === owner && it.asOuter }
            // outer 角色来自实际 parent 的注册顺序；纯读取不重新进入被冻结 parent 的可变上下文。
            val ownerStorage = owner.storage
            val ownerVariables = if (ownerIsOuter) ownerStorage.allTypeVariables.keys else
                ownerStorage.allTypeVariables.keys.take(ownerStorage.outerSystemVariablesPrefixSize)
            (ownerVariables + inherited).toSet()
        }
        // prefix 是角色集合的真实排列；安装前验证，不能仅用数量让中间 outer VM 变成 local。
        check(rebased.allTypeVariables.keys.take(outerVariables.size).toSet() == outerVariables)
        installStorage(rebased)
        storage.usesOuterCs = incoming.usesOuterCs
        storage.outerSystemVariablesPrefixSize = outerVariables.size
        val latest = owner.captureBaseline(baseline.asOuter)
        storage.parentBaseline = ConstraintSystemBaseline(owner, latest.storage, baseline.asOuter, listOf(baseline))
        @OptIn(AssertionsOnly::class)
        run { storage.outerCS = owner.storage }
    }

    /** detached 重放成功后提交；拒绝不会改变 live parent 的任何行、固定绑定或 fork 状态。 */
    fun integrateDerivedStorage(incoming: ConstraintStorage, protectParent: Boolean = true) {
        checkState(State.BUILDING, State.COMPLETION)
        val baseline = checkNotNull(provenance(incoming)?.parentBaseline) { "Child storage has no actual baseline" }
        installStorage(rebaseStorage(incoming, baseline, protectParent))
    }

    /** 森林后继覆盖由源操作及 VM 身份证明；旧 partial 不得复活 closed root 已消去的行。 */
    fun integrateInferenceForest(storages: Collection<ConstraintStorage>, requireClosed: Boolean) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        val snapshots = storages.map { from -> checkPartition(from); checkNotNull(provenance(from)); copyStorage(from) }
        // collector 合并与 actual source owner 接收 child 是不同角色；只有后者持有活动父变量。
        val activeParent = if (storage.usesOuterCs) storage.parentBaseline?.owner else null
        snapshots.forEach { incoming ->
            incoming.mutations.forEach mutationLoop@{ mutation ->
                val variable = mutation.fixedVariable ?: return@mutationLoop
                val key = variable.freshTypeConstructor()
                if (baselineContainsOwner(incoming.parentBaseline, this) &&
                    storage.mutations.none { it === mutation }) {
                    check(key !in storage.notFixedTypeVariables &&
                        !baselineHasUnfixedVariable(incoming.parentBaseline, this, key)) {
                        "A forest child fixed a variable owned by its active receiver"
                    }
                }
                if (activeParent != null &&
                    checkNotNull(provenance(activeParent.storage)).mutations.none { it === mutation }) {
                    check(key !in checkNotNull(storage.parentBaseline).storage.notFixedTypeVariables &&
                        key !in activeParent.storage.notFixedTypeVariables) {
                        "A forest root fixed a variable owned by the active parent"
                    }
                }
            }
        }
        fun covers(later: MutableConstraintStorage, earlier: MutableConstraintStorage): Boolean {
            val derived = later !== earlier &&
                (later.owner === earlier.owner || baselineContainsOwner(later.parentBaseline, earlier.owner)) &&
                earlier.allTypeVariables.all { (key, variable) -> later.allTypeVariables[key] === variable } &&
                isOrderedSubsequence(earlier.mutations, later.mutations) &&
                later.mutations.any { event -> earlier.mutations.none { it === event } }
            if (derived) {
                val substitutor = later.buildCurrentSubstitutor(this, emptyMap())
                earlier.fixedTypeVariables.forEach { (key, type) ->
                    check(substitutor.safeSubstitute(checkNotNull(later.fixedTypeVariables[key])) ==
                        substitutor.safeSubstitute(type)) { "A covering root changed an existing fixed binding" }
                }
            }
            return derived
        }
        val roots = snapshots.filter { earlier -> snapshots.none { later -> covers(later, earlier) } }
        // 根之间共享 VM 时也共享相同的源操作；独立同名 VM 不构成共享关系。
        for (root in roots) for (other in roots) {
            root.fixedTypeVariables.forEach { (key, type) ->
                if (key in other.fixedTypeVariables) {
                    val substitutor = root.buildCurrentSubstitutor(this, emptyMap())
                    check(substitutor.safeSubstitute(type) == substitutor.safeSubstitute(other.fixedTypeVariables.getValue(key))) {
                        "Independent completed roots disagree on a shared constructor"
                    }
                }
            }
        }
        check(!requireClosed || roots.all { it.notFixedTypeVariables.isEmpty() }) { "An independent forest has an uncompleted root" }
        val result = ConstraintSystemImpl(constraintInjector, typeSystemContext, languageVersionSettings)
        result.installStorage(captureStorage())
        roots.forEach { root ->
            root.mutations.forEach { mutation ->
                if (result.storage.mutations.none { it === mutation }) {
                    // 每个输入在覆盖筛选前已按 actual owner 和源操作身份验证。
                    result.replayMutation(mutation)
                }
            }
        }
        result.storage.mutations.filter { it.removal }.toList().forEach(result::replayMutation)
        checkPartition(result.storage)
        roots.forEach { root ->
            root.allTypeVariables.forEach { (key, variable) ->
                check(result.allTypeVariables[key] === variable) { "A forest root lost its actual VM" }
            }
            root.fixedTypeVariables.forEach { (key, type) ->
                val substitutor = result.buildCurrentSubstitutor()
                check(substitutor.safeSubstitute(checkNotNull(result.fixedTypeVariables[key])) == substitutor.safeSubstitute(type))
            }
        }
        check(!requireClosed || result.notFixedTypeVariables.isEmpty())
        installStorage(result.captureStorage())
    }

    /**
     * 当前约束系统状态。
     */
    private var state = State.BUILDING

    /**
     * 事务中新增的类型变量列表。
     */
    private val typeVariablesTransaction: MutableList<TypeVariableMarker> = SmartList()

    /**
     * proper type 判定的正向缓存。
     */
    private val properTypesCache: MutableSet<CangJieTypeMarker> = SmartSet.create()

    /**
     * proper type 判定的反向缓存。
     */
    private val notProperTypesCache: MutableSet<CangJieTypeMarker> = SmartSet.create()

    /**
     * 空交叉类型分类结果缓存。
     */
    private val intersectionTypesCache: MutableMap<Collection<CangJieTypeMarker>, EmptyIntersectionTypeInfo?> = mutableMapOf()

    // Cached value that should be reset on each new constraint or fork point
    /**
     * fork point 约束是否存在矛盾的缓存值。
     */
    private var hasContradictionInForkPointsCache: Boolean? = null

    /**
     * 在特定作用域内临时视为 proper type 的类型变量集合。
     */
    override var typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>? = null

    /**
     * 当前系统是否可通过非受限 builder inference 继续解析。
     */
    private var couldBeResolvedWithUnrestrictedBuilderInference: Boolean
        get() = storage.unrestrictedBuilderInference
        set(value) { storage.unrestrictedBuilderInference = value }

    /**
     * 当前系统是否已经进入补全阶段。
     */
    override var atCompletionState: Boolean = false

    /**
     * 是否允许把类型变量半固定到其他类型变量。
     */
    override var allowSemiFixationToOtherTypeVariables: Boolean = false

    /**
     * @see [org.cangnova.cangjie.resolve.calls.inference.components.VariableFixationFinder.Context.typeVariablesThatAreNotCountedAsProperTypes]
     * @see [org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirDeclarationsResolveTransformer.cfixInnerVariablesForProvideDelegateIfNeeded]
     */
    /**
     * 在 [block] 执行期间把指定 [typeVariables] 临时视为 proper type。
     */
    override fun <R> withTypeVariablesThatAreCountedAsProperTypes(
        typeVariables: Set<TypeConstructorMarker>,
        allowSemiFixationToOtherTypeVariables: Boolean,
        block: () -> R,
    ): R {
        checkState(State.BUILDING)
        // Cleaning cache is necessary because temporarily we change the meaning of what does "proper type" mean
        properTypesCache.clear()
        notProperTypesCache.clear()
        val previousAllowSemiFixationToOtherTypeVariables = this.allowSemiFixationToOtherTypeVariables

        require(typeVariablesThatAreCountedAsProperTypes == null) {
            val functionRef: KFunction<R> = ::withTypeVariablesThatAreCountedAsProperTypes
            "Currently there should be no nested ${functionRef.name} calls"
        }

        typeVariablesThatAreCountedAsProperTypes = typeVariables
        this.allowSemiFixationToOtherTypeVariables = allowSemiFixationToOtherTypeVariables

        val result = block()

        this.allowSemiFixationToOtherTypeVariables = previousAllowSemiFixationToOtherTypeVariables
        typeVariablesThatAreCountedAsProperTypes = null
        properTypesCache.clear()
        notProperTypesCache.clear()

        return result
    }

    /**
     * 约束系统生命周期状态。
     */
    private enum class State {
        /**
         * 正在构建约束。
         */
        BUILDING,

        /**
         * 正在事务中临时修改约束。
         */
        TRANSACTION,

        /**
         * 已冻结为只读存储。
         */
        FREEZED,

        /**
         * 正在执行约束系统补全。
         */
        COMPLETION
    }

    /*
     * If remove spread operator then call `checkState` will resolve to itself
     *   instead of fun checkState(vararg allowedState: State)
     */
    /**
     * 断言当前状态只能是 [a]。
     */
    private fun checkState(a: State) {
        if (state != State.FREEZED && !AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a))
    }

    /**
     * 断言当前状态只能是 [a] 或 [b]。
     */
    private fun checkState(a: State, b: State) {
        if (state != State.FREEZED && !AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b))
    }

    /**
     * 断言当前状态只能是 [a]、[b] 或 [c]。
     */
    private fun checkState(a: State, b: State, c: State) {
        if (state != State.FREEZED && !AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b, c))
    }

    /**
     * 断言当前状态只能是 [a]、[b]、[c] 或 [d]。
     */
    private fun checkState(a: State, b: State, c: State, d: State) {
        if (state != State.FREEZED && !AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b, c, d))
    }

    /**
     * 冻结生命周期边界始终检查；其余状态保持 Kotlin 的慢断言协议。
     */
    private fun checkState(vararg allowedState: State) {
        check(state != State.FREEZED || State.FREEZED in allowedState) {
            "Frozen constraint storage cannot enter a mutable context: " + allowedState.joinToString()
        }
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        assert(state in allowedState) {
            "State $state is not allowed. AllowedStates: ${allowedState.joinToString()}"
        }
    }

    /**
     * 当前约束系统收集到的错误列表。
     */
    override val errors: List<ConstraintSystemError>
        get() = storage.errors

    /**
     * 返回当前对象作为约束系统构建器。
     */
    override fun getBuilder() = apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) }

    /**
     * 冻结当前约束系统并返回只读存储视图。
     */
    override fun asReadOnlyStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.FREEZED)

        if (areThereContradictionsInForks()) {
            // If there are contradictions already, we might apply all the forks because CS is anyway already failed
            resolveForkPointsConstraints()
        }

        state = State.FREEZED
        return storage
    }

    /**
     * 进入约束系统补全上下文。
     */
    override fun asConstraintSystemCompleterContext() = apply {
        checkState(State.BUILDING)

        this.atCompletionState = true
    }

    /**
     * 返回 postponed 参数分析上下文。
     */
    override fun asPostponedArgumentsAnalyzerContext() = apply { checkState(State.BUILDING) }

    // ConstraintSystemOperation
    /**
     * 注册新的类型变量并为其创建可变约束集合。
     */
    override fun registerVariable(variable: TypeVariableMarker) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)

        transactionRegisterVariable(variable)
        storage.allTypeVariables.put(variable.freshTypeConstructor(), variable)
            ?.let { error("Type variable already registered: old: $it, new: $variable") }
        notProperTypesCache.clear()
        storage.notFixedTypeVariables[variable.freshTypeConstructor()] = MutableVariableWithConstraints(this, variable)
        if (sourceMutationDepth == 0) storage.mutations += newMutation(atCompletionState) { target ->
            val constructor = variable.freshTypeConstructor()
            val previous = target.allTypeVariables[constructor]
            if (previous == null) target.registerVariable(variable) else check(previous === variable)
        }
        inferenceLogger?.logNewVariable(variable, this)
    }

    /**
     * 将 [variable] 标记为 postponed 变量。
     */
    override fun markPostponedVariable(variable: TypeVariableMarker) {
        sourceMutation(action = { it.markPostponedVariable(variable) }) { storage.postponedTypeVariables += variable }
    }

    /**
     * 标记当前系统允许使用非受限 builder inference。
     */
    override fun markCouldBeResolvedWithUnrestrictedBuilderInference() {
        sourceMutation(action = { it.markCouldBeResolvedWithUnrestrictedBuilderInference() }) {
            couldBeResolvedWithUnrestrictedBuilderInference = true
        }
    }

    /**
     * 查询当前系统是否允许使用非受限 builder inference。
     */
    override fun couldBeResolvedWithUnrestrictedBuilderInference() =
        couldBeResolvedWithUnrestrictedBuilderInference

    /**
     * 取消 [variable] 的 postponed 标记。
     */
    override fun unmarkPostponedVariable(variable: TypeVariableMarker) {
        sourceMutation(action = { it.unmarkPostponedVariable(variable) }) { storage.postponedTypeVariables -= variable }
    }

    /**
     * 清空所有 postponed 变量标记。
     */
    override fun removePostponedVariables() {
        val retired = storage.postponedTypeVariables.toSet()
        sourceMutation(action = { target -> target.storage.postponedTypeVariables.removeAll(retired) }) {
            storage.postponedTypeVariables.clear()
        }
    }

    /**
     * 使用 [substitutor] 替换所有已经固定变量的结果类型。
     */
    override fun substituteFixedVariables(substitutor: TypeSubstitutorMarker) {
        // 替换器可以借用可变 map；journal 保存本次实际输入/输出，不能保留旧上下文。
        val replacements = storage.fixedTypeVariables.mapValues { (_, type) ->
            type to substitutor.safeSubstitute(type)
        }
        sourceMutation(action = { target ->
            val currentSubstitutor = target.buildCurrentSubstitutor()
            replacements.forEach { (key, types) ->
                val existing = checkNotNull(target.storage.fixedTypeVariables[key])
                val before = currentSubstitutor.safeSubstitute(types.first)
                val after = currentSubstitutor.safeSubstitute(types.second)
                check(currentSubstitutor.safeSubstitute(existing) == before ||
                    currentSubstitutor.safeSubstitute(existing) == after) {
                    "Fixed substitution no longer matches its actual source binding"
                }
                target.storage.fixedTypeVariables[key] = after
            }
        }) {
            replacements.forEach { (key, types) -> storage.fixedTypeVariables[key] = types.second }
        }
    }

    /**
     * 按顶层类型变量和期望类型路径缓存 postponed 参数构建出的函数期望类型。
     */
    override fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: CangJieTypeMarker,
    ) {
        val path = pathToExpectedType.toList()
        sourceMutation(action = { it.putBuiltFunctionalExpectedTypeForPostponedArgument(topLevelVariable, path, builtFunctionalType) }) {
            storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables[topLevelVariable to path] = builtFunctionalType
        }
    }

    /**
     * 按期望类型变量缓存 postponed 参数构建出的函数期望类型。
     */
    override fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: CangJieTypeMarker,
    ) {
        sourceMutation(action = { it.putBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable, builtFunctionalType) }) {
            storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables[expectedTypeVariable] = builtFunctionalType
        }
    }

    /**
     * 按顶层类型变量和期望类型路径读取 postponed 参数函数期望类型。
     */
    override fun getBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
    ) = storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables[topLevelVariable to pathToExpectedType]

    /**
     * 按期望类型变量读取 postponed 参数函数期望类型。
     */
    override fun getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable: TypeConstructorMarker) =
        storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables[expectedTypeVariable]

    /**
     * 向当前系统添加初始子类型约束。
     */
    override fun addSubtypeConstraint(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker, position: ConstraintPosition) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        generatingConstraints { constraintInjector.addInitialSubtypeConstraint(lowerType, upperType, position) }
    }

    /**
     * 向当前系统添加初始等价约束。
     */
    override fun addEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        generatingConstraints { constraintInjector.addInitialEqualityConstraint(a, b, position) }
    }

    /**
     * 删除指定类型变量上满足条件的约束。
     *
     * 该入口用于 PCLA fresh receiver 候选集合尚未收窄时撤销代表候选的临时 receiver 约束；
     * 其它变量和同一变量上的非匹配约束保持不变。
     */
    fun removeConstraintsForVariable(
        typeConstructor: TypeConstructorMarker,
        shouldRemove: (Constraint) -> Boolean,
    ) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        val row = storage.notFixedTypeVariables[typeConstructor] ?: return
        val retired = row.rawConstraintsSnapshot().filter(shouldRemove)
        if (retired.isEmpty()) return
        sourceMutation(removal = true, action = { target ->
            target.removeRetiredConstraints(typeConstructor, retired)
        }) {
            row.removeConstraints(shouldRemove)
            onNewConstraintOrForkPoint()
        }
    }

    /** 删除记录仅保存真实 source/kind/type；不捕获 PRIVATE row、任意谓词或其可变闭包。 */
    private fun removeRetiredConstraints(typeConstructor: TypeConstructorMarker, retired: List<Constraint>) {
        val row = storage.notFixedTypeVariables[typeConstructor] ?: return
        val substitutor = buildCurrentSubstitutor()
        row.removeConstraints { constraint ->
            retired.any { old ->
                old.position.initialConstraint === constraint.position.initialConstraint &&
                    old.kind == constraint.kind &&
                    substitutor.safeSubstitute(old.type) == substitutor.safeSubstitute(constraint.type)
            }
        }
        onNewConstraintOrForkPoint()
    }

    /**
     * 获取 [type] 对应类型变量的 proper 父类型构造器候选。
     */
    fun getProperSuperTypeConstructors(type: CangJieTypeMarker): List<TypeConstructorMarker> {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        val variableWithConstraints = notFixedTypeVariables[type.typeConstructor()] ?: return listOf(type.typeConstructor())

        return variableWithConstraints.constraints.mapNotNull {
            if (it.kind == ConstraintKind.LOWER) return@mapNotNull null
            it.type.typeConstructor().takeUnless { allTypeVariables.containsKey(it) }
        }
    }

    // ConstraintSystemBuilder
    /**
     * 在事务状态下记录新注册的类型变量，便于回滚时移除。
     */
    private fun transactionRegisterVariable(variable: TypeVariableMarker) {
        if (state != State.TRANSACTION) return
        if (variable.freshTypeConstructor() in storage.allTypeVariables) return
        typeVariablesTransaction.add(variable)
    }

    /**
     * 关闭事务并恢复事务开始前的系统状态。
     */
    private fun closeTransaction(beforeState: State, beforeTypeVariables: Int) {
        checkState(State.TRANSACTION)
        typeVariablesTransaction.trimToSize(beforeTypeVariables)
        state = beforeState
    }

    /**
     * 约束系统事务快照。
     *
     * 事务保存开始前的约束、错误、fork point、类型变量和深度限制状态，使失败分支可以
     * 精确回滚。
     */
    /** CS 事务快照同时恢复源删除和 settled fork；尾部截断不能恢复被主动移除的行。 */
    private inner class TransactionState(
        private val beforeState: State,
        private val beforeTypeVariablesTransactionSize: Int,
        private val beforeSnapshot: ConstraintSystemSnapshot,
        private val beforeForkCache: Boolean?,
        private val beforePostponedComputations: List<() -> Unit>,
        private val beforeCompletion: Boolean,
        private val beforeUnrestrictedBuilderInference: Boolean,
    ) : ConstraintSystemTransaction() {
        override fun closeTransaction() {
            checkState(State.TRANSACTION)
            typeVariablesTransaction.trimToSize(beforeTypeVariablesTransactionSize)
            state = beforeState
        }

        override fun rollbackTransaction() {
            installStorage(beforeSnapshot, preserveRows = true)
            postponedComputationsAfterAllVariablesAreFixed.clear()
            postponedComputationsAfterAllVariablesAreFixed.addAll(beforePostponedComputations)
            hasContradictionInForkPointsCache = beforeForkCache
            atCompletionState = beforeCompletion
            couldBeResolvedWithUnrestrictedBuilderInference = beforeUnrestrictedBuilderInference
            closeTransaction(beforeState, beforeTypeVariablesTransactionSize)
        }
    }

    override fun prepareTransaction(): ConstraintSystemTransaction {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return TransactionState(
            state, typeVariablesTransaction.size, captureStorage(), hasContradictionInForkPointsCache,
            postponedComputationsAfterAllVariablesAreFixed.toList(), atCompletionState,
            couldBeResolvedWithUnrestrictedBuilderInference,
        ).also { state = State.TRANSACTION }
    }

    // ConstraintSystemBuilder, KotlinConstraintSystemCompleter.Context
    /**
     * 当前系统是否已经出现普通约束或 fork point 约束矛盾。
     */
    override val hasContradiction: Boolean
        get() {
            checkState(
                State.FREEZED,
                State.BUILDING,
                State.COMPLETION,
                State.TRANSACTION
            )

            if (storage.hasContradiction) return true

        // Since 2.2 at each hasContradiction check, we make sure that all forks might be successfully resolved, too
        return areThereContradictionsInForks()
        }

    /**
     * 将外层约束系统加入当前系统，建立嵌套推断的外层变量前缀。
     */
    fun addOuterSystem(outerSystem: ConstraintStorage) {
        require(!storage.usesOuterCs)

        storage.usesOuterCs = true
        storage.parentBaseline = when (outerSystem) {
            is ConstraintSystemSnapshot -> ConstraintSystemBaseline(outerSystem.source, outerSystem, asOuter = true)
            is MutableConstraintStorage -> outerSystem.owner.captureBaseline(asOuter = true)
            else -> error("An outer CS must have an actual mutable owner")
        }
        storage.outerSystemVariablesPrefixSize = outerSystem.allTypeVariables.size
        @OptIn(AssertionsOnly::class)
        storage.outerCS = outerSystem

        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(outerSystem, isAddingOuter = true)

        doAddOtherSystem(outerSystem, mergeMode = false)
    }

    /**
     * 用 [baseSystem] 初始化当前约束系统的基础内容。
     */
    fun setBaseSystem(baseSystem: ConstraintStorage) {
        require(storage.allTypeVariables.isEmpty())
        storage.usesOuterCs = baseSystem.usesOuterCs
        storage.parentBaseline = when (baseSystem) {
            is ConstraintSystemSnapshot -> ConstraintSystemBaseline(baseSystem.source, baseSystem)
            // raw root 也是实际 source CS；没有上层 parent 时仍须保存克隆起点。
            is MutableConstraintStorage -> baseSystem.parentBaseline ?: baseSystem.owner.captureBaseline()
            else -> null
        }
        storage.outerSystemVariablesPrefixSize = baseSystem.outerSystemVariablesPrefixSize
        @OptIn(AssertionsOnly::class)
        storage.outerCS = (baseSystem as? MutableConstraintStorage)?.outerCS

        addOtherSystem(baseSystem)
    }

    /**
     * 进入全局补全前清除外层/内层变量的前缀分隔。
     */
    fun prepareForGlobalCompletion() {
        // There's no more separation of outer/inner variables once global completion starts
        storage.outerSystemVariablesPrefixSize = 0
    }

    /**
     * 将另一个约束系统内容追加到当前系统。
     */
    override fun addOtherSystem(otherSystem: ConstraintStorage) {
        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(otherSystem, isAddingOuter = false)

        if (storage.allTypeVariables.keys.any { it in otherSystem.allTypeVariables }) {
            integrateInferenceForest(listOf(otherSystem), requireClosed = false)
        } else doAddOtherSystem(otherSystem, mergeMode = false)
    }

    /**
     * 以合并模式加入另一个约束系统，重复约束和错误会按身份去重。
     */
    @UnstableSystemMergeMode
    override fun mergeOtherSystem(otherSystem: ConstraintStorage) {
        // 合并仍归 CS 所有；来源重放让旧 partial 与后继 closed storage 可以安全汇合。
        integrateInferenceForest(listOf(otherSystem), requireClosed = false)
    }

    /**
     * This function is only expected to be called when [otherSystem] is a superset of this CS,
     * or in other words _this_ CS has used as a base/outer CS of the [otherSystem].
     *
     * Or one might say that [otherSystem] is expected to be a clone of the current CS with some additions: new variables, constraints, etc.
     */
    /**
     * 用 [otherSystem] 的内容替换当前系统内容。
     *
     * 调用方必须保证 [otherSystem] 是当前系统的超集或以当前系统为基础派生出的克隆。
     */
    fun replaceContentWith(otherSystem: ConstraintStorage) {
        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(otherSystem, isAddingOuter = false)

        if (otherSystem === storage) return
        val baseline = provenance(otherSystem)?.parentBaseline
        if (baseline != null && (baseline.owner === this ||
                baselineContainsOwner(provenance(baseline.storage)?.parentBaseline, this))) {
            integrateDerivedStorage(otherSystem, protectParent = false)
            return
        }
        check(otherSystem.allTypeVariables.all { (key, variable) ->
            storage.allTypeVariables[key]?.let { it === variable } != false
        })
        check(otherSystem.allTypeVariables.keys.containsAll(storage.allTypeVariables.keys))
        check(isOrderedSubsequence(storage.mutations, checkNotNull(provenance(otherSystem)).mutations)) {
            "Replacement requires a genuinely derived storage"
        }
        installStorage(otherSystem)
    }

    /**
     * 按追加或合并模式把 [otherSystem] 的内部状态并入当前存储。
     */
    private fun doAddOtherSystem(otherSystem: ConstraintStorage, mergeMode: Boolean) {
        checkPartition(otherSystem)
        otherSystem.allTypeVariables.forEach { (key, variable) ->
            storage.allTypeVariables[key]?.let { check(it === variable) { "Constraint import rebound a constructor" } }
        }
        otherSystem.notFixedTypeVariables.keys.forEach { key ->
            check(key !in storage.fixedTypeVariables) { "A raw obsolete import resurrected a fixed row; use source rebase" }
        }
        otherSystem.fixedTypeVariables.forEach { (key, type) ->
            storage.fixedTypeVariables[key]?.let { check(it == type) }
            check(key !in storage.notFixedTypeVariables) { "A raw import needs source fixation before installing its fixed binding" }
        }
        if (otherSystem.allTypeVariables.isNotEmpty()) {
            otherSystem.allTypeVariables.forEach {
                transactionRegisterVariable(it.value)
            }
            storage.allTypeVariables.putAll(otherSystem.allTypeVariables)
            notProperTypesCache.clear()
        }

        for ((k, v) in otherSystem.approximatorCaches) {
            storage.approximatorCaches.getOrPut(k) { AbstractTypeApproximator.Cache() } += v
        }

        for ((variable, constraints) in otherSystem.notFixedTypeVariables) {
            if (!mergeMode) {
                notFixedTypeVariables[variable] = if (constraints is MutableVariableWithConstraints) constraints.snapshot(this) else MutableVariableWithConstraints(this, constraints)
            } else {
                val previous = notFixedTypeVariables[variable]
                if (previous != null) {
                    @OptIn(UnstableSystemMergeMode::class)
                    notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, previous, constraints)
                } else {
                    notFixedTypeVariables[variable] = if (constraints is MutableVariableWithConstraints) constraints.snapshot(this) else MutableVariableWithConstraints(this, constraints)
                }
            }
        }

        for ((variable, variablesThatReferenceGivenOne) in otherSystem.typeVariableDependencies) {
            if (!mergeMode || variable !in typeVariableDependencies) {
                typeVariableDependencies[variable] = variablesThatReferenceGivenOne.toMutableSet()
            } else {
                typeVariableDependencies[variable]?.addAll(variablesThatReferenceGivenOne)
            }
        }

        // Merge mode: filtering identical constraints
        if (mergeMode) {
            storage.initialConstraints.addAllDistinct(otherSystem.initialConstraints)
            storage.constraintsFromAllForkPoints.addAllDistinct(otherSystem.constraintsFromAllForkPoints)
            storage.errors.addAllDistinct(otherSystem.errors)
        } else {
            storage.initialConstraints.addAll(otherSystem.initialConstraints)
            storage.constraintsFromAllForkPoints.addAll(otherSystem.constraintsFromAllForkPoints)
            storage.errors.addAll(otherSystem.errors)
        }

        storage.maxTypeDepthFromInitialConstraints =
            max(storage.maxTypeDepthFromInitialConstraints, otherSystem.maxTypeDepthFromInitialConstraints)
        // Keys are compared by identity only.
        // Sometimes we create structurally identical type variables (at least in K2),
        // and they should be considered different.
        storage.fixedTypeVariables.putAll(otherSystem.fixedTypeVariables)
        storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables.putAll(otherSystem.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables)
        storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables.putAll(otherSystem.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables)
        provenance(otherSystem)?.let { origin ->
            storage.unrestrictedBuilderInference = storage.unrestrictedBuilderInference || origin.unrestrictedBuilderInference
            origin.mutations.forEach { mutation -> if (storage.mutations.none { it === mutation }) storage.mutations += mutation }
            origin.resolvedForks.forEach { outcome ->
                if (storage.resolvedForks.none { it.data === outcome.data }) storage.resolvedForks += outcome
            }
        }
        // K1-only, so merge isn't important here
        storage.postponedTypeVariables.addAll(otherSystem.postponedTypeVariables)

        hasContradictionInForkPointsCache = null
    }

    /**
     * 以身份集合语义把 [other] 加入当前列表并去重。
     */
    private fun <T> MutableList<T>.addAllDistinct(other: List<T>) {
        val set = identityHashSetFromSum(this, other)
        clear()
        addAll(set)
    }

    /**
     * 校验外层约束系统相关的不变量。
     */
    @AssertionsOnly
    private fun runOuterCSRelatedAssertions(otherSystem: ConstraintStorage, isAddingOuter: Boolean) {
        if (!otherSystem.usesOuterCs) return

        // When integrating a child system back, it's ok that for root CS, `storage.usesOuterCs == false`
        if ((otherSystem as? MutableConstraintStorage)?.outerCS === storage) return

        require(storage.usesOuterCs)

        if (!isAddingOuter) {
            require(storage.outerSystemVariablesPrefixSize == otherSystem.outerSystemVariablesPrefixSize) {
                "Expected to be ${otherSystem.outerSystemVariablesPrefixSize}, but ${storage.outerSystemVariablesPrefixSize} found"
            }
        }
    }

    // ResultTypeResolver.Context, ConstraintSystemBuilder
    /**
     * 判断 [type] 是否不依赖当前系统未固定变量。
     */
    override fun isProperType(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        if (storage.allTypeVariables.isEmpty()) return true
        if (notProperTypesCache.contains(type)) return false
        if (properTypesCache.contains(type)) return true
        return isProperTypeImpl(type).also {
            (if (it) properTypesCache else notProperTypesCache).add(type)
        }
    }

    /**
     * 执行 proper type 判定的实际递归检查。
     */
    private fun isProperTypeImpl(type: CangJieTypeMarker): Boolean =
        !type.contains {
            val typeToCheck = it


            if (typeVariablesThatAreCountedAsProperTypes?.contains(typeToCheck.typeConstructor()) == true) {
                return@contains false
            }

            return@contains storage.allTypeVariables.containsKey(typeToCheck.typeConstructor())
        }

    /**
     * 判断 [type] 是否是当前未固定类型变量。
     */
    override fun isTypeVariable(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return notFixedTypeVariables.containsKey(type.typeConstructor())
    }

    /**
     * 判断 [typeVariable] 是否处于 postponed 状态。
     */
    override fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return typeVariable in postponedTypeVariables
    }

    // ConstraintInjector.Context, KotlinConstraintSystemCompleter.Context
    /**
     * 当前系统注册的全部类型变量。
     */
    override val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.allTypeVariables
        }

    /**
     * 初始约束允许的最大类型深度。
     */
    override var maxTypeDepthFromInitialConstraints: Int
        get() = storage.maxTypeDepthFromInitialConstraints
        set(value) {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            storage.maxTypeDepthFromInitialConstraints = value
        }

    /**
     * 记录一条初始约束。
     */
    override fun addInitialConstraint(initialConstraint: InitialConstraint) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        storage.initialConstraints.add(initialConstraint)
        if (sourceMutationDepth == 0) storage.mutations += newMutation(atCompletionState) { target ->
            with(target) {
                val substitutor = buildCurrentSubstitutor()
                generatingConstraints {
                    constraintInjector.replayInitialConstraint(initialConstraint,
                        substitutor.safeSubstitute(initialConstraint.a), substitutor.safeSubstitute(initialConstraint.b))
                }
            }
        }
    }

    // ConstraintInjector.Context, FixationOrderCalculator.Context
    /** 向固定顺序策略提供原始约束，保留声明边的方向及来源。 */
    override val initialConstraints: List<InitialConstraint>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.initialConstraints
        }

    /**
     * 当前尚未固定的类型变量及其可变约束集合。
     */
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.notFixedTypeVariables
        }

    /**
     * @see org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.typeVariableDependencies
     */
    /**
     * 类型变量依赖索引。
     */
    override val typeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.typeVariableDependencies
        }

    /**
     * 已固定类型变量到结果类型的映射。
     */
    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.fixedTypeVariables
        }

    /**
     * 当前 postponed 类型变量列表。
     */
    override val postponedTypeVariables: List<TypeVariableMarker>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.postponedTypeVariables
        }

    /**
     * 外层约束系统变量在全部变量中的前缀数量。
     */
    override val outerSystemVariablesPrefixSize: Int
        get() = storage.outerSystemVariablesPrefixSize

    /**
     * 当前收集的 fork point 约束集合。
     */
    override val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.constraintsFromAllForkPoints
        }

    /**
     * This function tries to find the solution (set of constraints) that is consistent with some branch of each fork.
     * And those constraints are being immediately applied to the system
     */
    /**
     * 解析所有 fork point 约束并把每个 fork point 的最佳分支应用到当前系统。
     */
    override fun resolveForkPointsConstraints() {
        if (constraintsFromAllForkPoints.isEmpty()) return
        val sources = constraintsFromAllForkPoints.map { it.first.initialConstraint }.toList()
        sourceMutation(action = { it.resolveForkPointsFromSources(sources) }) {
            generatingConstraints { resolveForkPointsFromSources(sources) }
        }
    }

    private fun resolveForkPointsFromSources(sources: List<InitialConstraint>) {
        val allForkPointsData = constraintsFromAllForkPoints.filter { entry -> sources.any { it === entry.first.initialConstraint } }
        constraintsFromAllForkPoints.removeAll { entry -> allForkPointsData.any { it === entry } }

        // There may be multiple fork points:
        // - One from subtyping A<Int> & A<T> <: A<Xv>
        // - Another one from B<String> & B<F> <: B<Yv>
        // Each of them defines two sets of constraints, e.g. for the first for point:
        // 1. {Xv=Int} – is a one-element set (but potentially there might be more constraints in the set)
        // 2. {Xv=T} – second constraints set
        for ((position, forkPointData) in allForkPointsData) {
            applyTheBestBranchFromForkPoint(forkPointData, position)
        }
    }

    /**
     * 新增约束或 fork point 后清空矛盾缓存。
     */
    override fun onNewConstraintOrForkPoint() {
        hasContradictionInForkPointsCache = null
    }

    /**
     * Checks if the current state of forked constraints is not contradictory.
     *
     * That function is expected to be pure, i.e., it should leave the system in the same state it was found before the call.
     *
     */
    /**
     * 检查当前 fork point 约束是否存在无可成功分支的矛盾。
     */
    fun areThereContradictionsInForks(): Boolean {
        // Before freezing, we guarantee to apply contradictions to the regular storage if there are any
        // (see ConstraintSystemImpl.asReadOnlyStorage)
        if (state == State.FREEZED) return false

        if (constraintsFromAllForkPoints.isEmpty()) return false

        hasContradictionInForkPointsCache?.let { return it }

        val allForkPointsData = constraintsFromAllForkPoints.toList()
        constraintsFromAllForkPoints.clear()

        val isThereAnyUnsuccessful: Boolean
        runTransaction {
            isThereAnyUnsuccessful = allForkPointsData.any { (position, forkPointData) ->
                !applyTheBestBranchFromForkPoint(forkPointData, position)
            }

            false
        }

        constraintsFromAllForkPoints.addAll(allForkPointsData)

        return isThereAnyUnsuccessful.also { hasContradictionInForkPointsCache = it }
    }

    /**
     * Applies the first successful branch if there's any.
     * Otherwise, applies just the first branch (containing contradictions)
     *
     * @return true if there is a successful constraint set for the fork point.
     */
    /**
     * 对单个 fork point 应用第一个成功分支；没有成功分支时应用第一个失败分支以保留错误。
     */
    private fun applyTheBestBranchFromForkPoint(
        forkPointData: ForkPointData,
        position: IncorporationConstraintPosition,
    ): Boolean {
        var chosenBranch: ForkPointBranchDescription? = null
        val isSuccessful = forkPointData.any { constraintSetForForkBranch ->
            runTransaction {
                applyForkPointBranch(constraintSetForForkBranch, position)

                (!storage.hasContradiction).also { if (it) chosenBranch = constraintSetForForkBranch }
            }
        }

        if (!isSuccessful) {
            applyForkPointBranch(forkPointData.first(), position)
            chosenBranch = forkPointData.first()
        }
        storage.resolvedForks += ConstraintSystemForkOutcome(position, forkPointData, checkNotNull(chosenBranch), isSuccessful)
        return isSuccessful
    }

    /**
     * 把 fork point 的某个分支约束集合应用到当前系统。
     */
    private fun applyForkPointBranch(
        constraintSetForForkBranch: ForkPointBranchDescription,
        position: IncorporationConstraintPosition,
    ) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        constraintInjector.processGivenForkPointBranchConstraints(
            constraintSetForForkBranch,
            position,
        )

        // Some new fork points constraints might be introduced, and we apply them immediately because we anyway at the
        // completion state (as we already started resolving them)
        resolveForkPointsConstraints()
    }

    // ConstraintInjector.Context, KotlinConstraintSystemCompleter.Context
    /**
     * 记录一条约束系统错误并写入推断日志。
     */
    override fun addError(error: ConstraintSystemError) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        storage.errors.add(error)
        if (sourceMutationDepth == 0 && constraintGenerationDepth == 0) {
            storage.mutations += newMutation(atCompletionState) { it.addError(error) }
        }
        inferenceLogger?.logError(error, this)
    }

    // KotlinConstraintSystemCompleter.Context
    /**
     * 使用 [resultType] 固定 [variable]，并清理其他约束中对该变量的引用。
     */
    override fun fixVariable(
        variable: TypeVariableMarker,
        resultType: CangJieTypeMarker,
        position: FixVariableConstraintPosition<*>,
    ): Unit = with(utilContext) {
        fixVariableWithInitialConstraint(variable, resultType, position,
            InitialConstraint(variable.defaultType(), resultType, ConstraintKind.EQUALITY, position))
    }

    private fun fixVariableWithInitialConstraint(
        variable: TypeVariableMarker,
        resultType: CangJieTypeMarker,
        position: FixVariableConstraintPosition<*>,
        initialConstraint: InitialConstraint,
    ): Unit = with(utilContext) {
        checkState(State.BUILDING, State.COMPLETION)
        val constructor = variable.freshTypeConstructor()
        require(constructor !in outerTypeVariables.orEmpty()) {
            "Outer type variables must remain owned by their active parent"
        }
        sourceMutation(fixedVariable = variable, action = { target ->
            val existing = target.fixedTypeVariables[variable.freshTypeConstructor()]
            if (existing == null) target.fixVariableWithInitialConstraint(variable, resultType, position, initialConstraint)
            else check(target.buildCurrentSubstitutor().safeSubstitute(existing) ==
                target.buildCurrentSubstitutor().safeSubstitute(resultType)) { "Source fixation disagrees with actual owner" }
        }) {
            generatingConstraints {
                checkInferredEmptyIntersection(variable, resultType)

                val substitutor = buildCurrentSubstitutor()
                constraintInjector.replayInitialConstraint(initialConstraint,
                    substitutor.safeSubstitute(initialConstraint.a), substitutor.safeSubstitute(initialConstraint.b))

                val freshTypeConstructor = variable.freshTypeConstructor()
                val variableWithConstraints =
                    notFixedTypeVariables.remove(freshTypeConstructor) ?: error("Seems that $variable is being fixed second time")

                outerTypeVariables?.let { outerVariables ->
                    require(freshTypeConstructor !in outerVariables) {
                        "Outer type variables are not assumed to be fixed during nested calls analysis, but $variable is being fixed"
                    }
                }

                for (otherVariableWithConstraints in notFixedTypeVariables.values) {
                    otherVariableWithConstraints.removeConstraints { it.type.containsTypeVariable(freshTypeConstructor) }
                }

                storage.fixedTypeVariables[freshTypeConstructor] = resultType
                inferenceLogger?.logReadiness(InferenceLogger.FixationLogRecord(emptyMap(), variable), this@ConstraintSystemImpl)
                inferenceLogger?.logFixVariable(variable, resultType, this@ConstraintSystemImpl)

                doPostponedComputationsIfAllVariablesAreFixed()
            }
        }
    }

    /**
     * 获取指定类型集合的空交叉类型分类信息。
     */
    override fun getEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo? {
        if (types in intersectionTypesCache)
            return intersectionTypesCache.getValue(types)

        return computeEmptyIntersectionTypeKind(types).also {
            intersectionTypesCache[types] = it
        }
    }

    /**
     * 检查固定结果是否推断出了新的空交叉类型，并按需要替换为补全阶段诊断。
     */
    private fun checkInferredEmptyIntersection(variable: TypeVariableMarker, resultType: CangJieTypeMarker) {
        val intersectionTypeConstructor = resultType.typeConstructor().takeIf { it is IntersectionTypeConstructorMarker } ?: return
        val upperTypes = intersectionTypeConstructor.supertypes()

        // Diagnostic with these incompatible types has already been reported at the resolution stage
        if (upperTypes.size <= 1 || storage.errors.any { it is InferredEmptyIntersection && it.incompatibleTypes == upperTypes })
            return

        val emptyIntersectionTypeInfo = getEmptyIntersectionTypeKind(upperTypes) ?: return

        // Remove existing errors from the resolution stage because a completion stage error is always more precise
        storage.errors.removeIf { it is InferredEmptyIntersection }

        val isInferredEmptyIntersectionForbidden = true
        val errorFactory = if (emptyIntersectionTypeInfo.kind.isDefinitelyEmpty && isInferredEmptyIntersectionForbidden)
            ::InferredEmptyIntersectionError
        else ::InferredEmptyIntersectionWarning

        addError(
            errorFactory(upperTypes.toList(), emptyIntersectionTypeInfo.casingTypes.toList(), variable, emptyIntersectionTypeInfo.kind)
        )
    }

    /**
     * 在所有类型变量固定后延迟检查 OnlyInputTypes 约束。
     */
    private fun ConstraintSystemUtilContext.postponeOnlyInputTypesCheck(
        variableWithConstraints: MutableVariableWithConstraints,
        resultType: CangJieTypeMarker,
    ) {
        // `OnlyInputTypes` 依赖最终替换后的输入类型集合，必须等所有相关类型变量 fix 完成后再统一检查。
        if (variableWithConstraints.typeVariable.hasOnlyInputTypesAttribute()) {
            postponedComputationsAfterAllVariablesAreFixed.add {
                checkOnlyInputTypesAnnotation(variableWithConstraints, resultType)
            }
        }
    }


    /**
     * 当所有变量均已固定时执行延迟计算。
     */
    private fun doPostponedComputationsIfAllVariablesAreFixed() {
        if (notFixedTypeVariables.isEmpty()) {
            postponedComputationsAfterAllVariablesAreFixed.forEach { it() }
        }
    }

    /**
     * 对输入类型执行当前替换，并按约束方向执行必要近似。
     */
    private fun CangJieTypeMarker.substituteAndApproximateIfNecessary(
        substitutor: TypeSubstitutorMarker,
        approximator: AbstractTypeApproximator,
        constraintKind: ConstraintKind,
    ): CangJieTypeMarker {
        val doesInputTypeContainsOtherVariables = this.contains { it.typeConstructor() is TypeVariableTypeConstructorMarker }
        val substitutedType = if (doesInputTypeContainsOtherVariables) substitutor.safeSubstitute(this) else this
        // Appoximation here is the same as ResultTypeResolver do
        val approximatedType = when (constraintKind) {
            ConstraintKind.LOWER ->
                approximator.approximateToSuperType(substitutedType, TypeApproximatorConfiguration.InternalTypesApproximation)
            ConstraintKind.UPPER ->
                approximator.approximateToSubType(substitutedType, TypeApproximatorConfiguration.InternalTypesApproximation)
            ConstraintKind.EQUALITY -> substitutedType
        } ?: substitutedType

        return approximatedType
    }

    /**
     * 检查 OnlyInputTypes 类型变量的固定结果是否等于某个投影后的输入类型。
     */
    private fun checkOnlyInputTypesAnnotation(variableWithConstraints: MutableVariableWithConstraints, resultType: CangJieTypeMarker) {
        val substitutor = buildCurrentSubstitutor()
        val approximator = constraintInjector.typeApproximator
        val projectedInputCallTypes = variableWithConstraints.getProjectedInputCallTypes(utilContext)
        val isResultTypeEqualSomeInputType = projectedInputCallTypes.any { (inputType, constraintKind) ->
            val inputTypeConstructor = inputType.typeConstructor()
            val otherResultType = inputType.substituteAndApproximateIfNecessary(substitutor, approximator, constraintKind)

            if (AbstractTypeChecker.equalTypes(this, resultType, otherResultType)) return@any true
            if (!inputTypeConstructor.isIntersection()) return@any false

            inputTypeConstructor.supertypes().any {
                val intersectionComponentResultType = it.substituteAndApproximateIfNecessary(substitutor, approximator, constraintKind)
                AbstractTypeChecker.equalTypes(this, resultType, intersectionComponentResultType)
            }
        }
        if (!isResultTypeEqualSomeInputType) {
            addError(OnlyInputTypesDiagnostic(variableWithConstraints.typeVariable))
        }
    }

    // KotlinConstraintSystemCompleter.Context, PostponedArgumentsAnalyzer.Context
    /**
     * 判断 [type] 是否可以被视为 proper type。
     */
    override fun canBeProper(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains { storage.notFixedTypeVariables.containsKey(it.typeConstructor()) }
    }

    /**
     * 判断 [type] 是否只包含已固定变量或 postponed 变量。
     */
    override fun containsOnlyFixedOrPostponedVariables(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains {
            val typeConstructor = it.typeConstructor()
            val variable = storage.notFixedTypeVariables[typeConstructor]?.typeVariable
            variable !in storage.postponedTypeVariables && storage.notFixedTypeVariables.containsKey(typeConstructor)
        }
    }

    /**
     * 判断 [type] 是否只包含已经固定的类型变量。
     */
    override fun containsOnlyFixedVariables(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains {
            val typeConstructor = it.typeConstructor()
            storage.notFixedTypeVariables.containsKey(typeConstructor)
        }
    }

    // PostponedArgumentsAnalyzer.Context
    /**
     * 构建当前已固定变量的替换器。
     */
    override fun buildCurrentSubstitutor(): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return buildCurrentSubstitutor(emptyMap())
    }

    /**
     * 构建当前已固定变量和 [additionalBindings] 合并后的替换器。
     */
    override fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.buildCurrentSubstitutor(this, additionalBindings)
    }

    /**
     * 构建把未固定变量替换为不可参与子类型关系 stub type 的替换器。
     */
    override fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.buildNotFixedVariablesToNonSubtypableTypesSubstitutor(this)
    }



    /**
     * 为 postponed 变量创建 builder inference stub type 绑定。
     */
    override fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker> {
        checkState(State.BUILDING, State.COMPLETION)
        // TODO: SUB
        return storage.postponedTypeVariables.associateWith { createStubTypeForBuilderInference(it) }
    }

    /**
     * 返回当前约束系统存储。
     */
    override fun currentStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage
    }

    /**
     * 当前系统是否使用了外层约束系统。
     */
    val usesOuterCs: Boolean get() = storage.usesOuterCs

    // PostponedArgumentsAnalyzer.Context
    /**
     * 判断 [type] 对应变量是否存在 Unit 上界或等价约束。
     */
    override fun hasUpperOrEqualUnitConstraint(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.FREEZED)
        val constraints = storage.notFixedTypeVariables[type.typeConstructor()]?.constraints ?: return false
        return constraints.any {
            (it.kind == ConstraintKind.UPPER || it.kind == ConstraintKind.EQUALITY) && !it.isNoInfer &&
                    it.type.isUnit()
        }
    }

    /**
     * 从所有约束中移除引用指定 postponed stub 类型变量的约束。
     */
    override fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>) {
        val retiredVariables = postponedTypeVariables.toSet()
        val retiredByVariable = storage.notFixedTypeVariables.mapValues { (_, row) ->
            row.rawConstraintsSnapshot().filter { constraint ->
                constraint.type.contains { it is StubTypeMarker && it.getOriginalTypeVariable() in retiredVariables }
            }
        }
        sourceMutation(removal = true, action = { target ->
            retiredByVariable.forEach { (key, retired) -> target.removeRetiredConstraints(key, retired) }
        }) {
            retiredByVariable.forEach { (key, retired) -> removeRetiredConstraints(key, retired) }
        }
    }

    /**
     * 在类型变量依赖索引中记录一条约束引用关系。
     */
    override fun recordTypeVariableReferenceInConstraint(
        constraintOwner: TypeConstructorMarker,
        referencedVariable: TypeConstructorMarker,
    ) {
        typeVariableDependencies.getOrPut(referencedVariable) { mutableSetOf() }
            .add(constraintOwner)
    }
}

/** 真实 source CS 的不可变基线，保持 VM、constructor 与 InferenceScope 身份。 */
class ConstraintSystemBaseline internal constructor(
    val owner: ConstraintSystemImpl,
    val storage: ConstraintSystemSnapshot,
    internal val asOuter: Boolean = false,
    /** 当前同步 owner 改为祖先时，中间 source 的原始快照继续承担身份、历史和固定保护职责。 */
    internal val retainedSources: List<ConstraintSystemBaseline> = emptyList(),
)

/** 对外为只读 storage；模块内部保存 raw row 和实际来源，用于重放及事务。 */
class ConstraintSystemSnapshot internal constructor(
    internal val source: ConstraintSystemImpl,
    internal val content: MutableConstraintStorage,
) : ConstraintStorage by content

internal class ConstraintSystemMutation(
    val completion: Boolean,
    val removal: Boolean = false,
    val fixedVariable: TypeVariableMarker? = null,
    val properVariables: Set<TypeConstructorMarker>?,
    val allowSemiFixation: Boolean,
    val action: (ConstraintSystemImpl) -> Unit,
)

/** fork 队列清空后仍保留真正选择的分支和成功/错误 outcome；与事务快照一起恢复。 */
internal class ConstraintSystemForkOutcome(
    val position: IncorporationConstraintPosition,
    val data: ForkPointData,
    val branch: ForkPointBranchDescription,
    val successful: Boolean,
)
