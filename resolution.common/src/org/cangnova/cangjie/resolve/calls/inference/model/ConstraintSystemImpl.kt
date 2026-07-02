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
import org.cangnova.cangjie.types.TypeApproximatorCachesPerConfiguration
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
    private val storage = MutableConstraintStorage()

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
    private var couldBeResolvedWithUnrestrictedBuilderInference: Boolean = false

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
     * @see [org.cangnova.cangjie.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
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
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a))
    }

    /**
     * 断言当前状态只能是 [a] 或 [b]。
     */
    private fun checkState(a: State, b: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b))
    }

    /**
     * 断言当前状态只能是 [a]、[b] 或 [c]。
     */
    private fun checkState(a: State, b: State, c: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b, c))
    }

    /**
     * 断言当前状态只能是 [a]、[b]、[c] 或 [d]。
     */
    private fun checkState(a: State, b: State, c: State, d: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b, c, d))
    }

    /**
     * 在慢断言开启时校验当前状态属于 [allowedState]。
     */
    private fun checkState(vararg allowedState: State) {
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
        inferenceLogger?.logNewVariable(variable, this)
    }

    /**
     * 将 [variable] 标记为 postponed 变量。
     */
    override fun markPostponedVariable(variable: TypeVariableMarker) {
        storage.postponedTypeVariables += variable
    }

    /**
     * 标记当前系统允许使用非受限 builder inference。
     */
    override fun markCouldBeResolvedWithUnrestrictedBuilderInference() {
        couldBeResolvedWithUnrestrictedBuilderInference = true
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
        storage.postponedTypeVariables -= variable
    }

    /**
     * 清空所有 postponed 变量标记。
     */
    override fun removePostponedVariables() {
        storage.postponedTypeVariables.clear()
    }

    /**
     * 使用 [substitutor] 替换所有已经固定变量的结果类型。
     */
    override fun substituteFixedVariables(substitutor: TypeSubstitutorMarker) {
        storage.fixedTypeVariables.replaceAll { _, type -> substitutor.safeSubstitute(type) }
    }

    /**
     * 按顶层类型变量和期望类型路径缓存 postponed 参数构建出的函数期望类型。
     */
    override fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: CangJieTypeMarker,
    ) {
        storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables[topLevelVariable to pathToExpectedType] =
            builtFunctionalType
    }

    /**
     * 按期望类型变量缓存 postponed 参数构建出的函数期望类型。
     */
    override fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: CangJieTypeMarker,
    ) {
        storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables[expectedTypeVariable] = builtFunctionalType
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
        constraintInjector.addInitialSubtypeConstraint(lowerType, upperType, position)
    }

    /**
     * 向当前系统添加初始等价约束。
     */
    override fun addEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        constraintInjector.addInitialEqualityConstraint(a, b, position)
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
        val variableWithConstraints = storage.notFixedTypeVariables[typeConstructor] ?: return
        variableWithConstraints.removeConstraints(shouldRemove)
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
    private inner class TransactionState(
        /**
         * 事务开始前的约束系统状态。
         */
        private val beforeState: State,

        /**
         * 事务开始前初始约束数量。
         */
        private val beforeInitialConstraintCount: Int,

        /**
         * 事务开始前错误数量。
         */
        private val beforeErrorsCount: Int,

        /**
         * 事务开始前允许的最大初始类型深度。
         */
        private val beforeMaxTypeDepthFromInitialConstraints: Int,

        /**
         * 事务开始前已记录的新类型变量数量。
         */
        private val beforeTypeVariablesTransactionSize: Int,

        /**
         * 事务开始前每个变量拥有的原始约束数量。
         */
        private val beforeConstraintCountByVariables: Map<TypeConstructorMarker, Int>,

        /**
         * 事务开始前 fork point 约束数量。
         */
        private val beforeConstraintsFromAllForks: Int,

        /**
         * 事务开始前 fork point 矛盾缓存值。
         */
        private val beforeHasContradictionInForkPointsCache: Boolean?,
    ) : ConstraintSystemTransaction() {
        /**
         * 提交事务并恢复事务开始前的外层状态。
         */
        override fun closeTransaction() {
            checkState(State.TRANSACTION)
            typeVariablesTransaction.trimToSize(beforeTypeVariablesTransactionSize)
            state = beforeState
        }

        /**
         * 回滚事务中新增的类型变量、约束、错误和 fork point 数据。
         */
        override fun rollbackTransaction() {
            for (addedTypeVariable in typeVariablesTransaction.subList(beforeTypeVariablesTransactionSize, typeVariablesTransaction.size)) {
                storage.allTypeVariables.remove(addedTypeVariable.freshTypeConstructor())
                storage.notFixedTypeVariables.remove(addedTypeVariable.freshTypeConstructor())
            }
            storage.maxTypeDepthFromInitialConstraints = beforeMaxTypeDepthFromInitialConstraints
            storage.errors.trimToSize(beforeErrorsCount)
            storage.constraintsFromAllForkPoints.trimToSize(beforeConstraintsFromAllForks)

            val addedInitialConstraints = storage.initialConstraints.subList(
                beforeInitialConstraintCount,
                storage.initialConstraints.size
            )

            for (variableWithConstraint in storage.notFixedTypeVariables.values) {
                val sinceIndexToRemoveConstraints =
                    beforeConstraintCountByVariables[variableWithConstraint.typeVariable.freshTypeConstructor()]
                if (sinceIndexToRemoveConstraints != null) {
                    variableWithConstraint.removeLastConstraints(sinceIndexToRemoveConstraints)
                }
            }

            addedInitialConstraints.clear() // remove constraint from storage.initialConstraints

            hasContradictionInForkPointsCache = beforeHasContradictionInForkPointsCache

            closeTransaction(beforeState, beforeTypeVariablesTransactionSize)
        }
    }

    /**
     * 创建一个新的约束系统事务。
     */
    override fun prepareTransaction(): ConstraintSystemTransaction {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return TransactionState(
            beforeState = state,
            beforeInitialConstraintCount = storage.initialConstraints.size,
            beforeErrorsCount = storage.errors.size,
            beforeMaxTypeDepthFromInitialConstraints = storage.maxTypeDepthFromInitialConstraints,
            beforeTypeVariablesTransactionSize = typeVariablesTransaction.size,
            beforeConstraintCountByVariables = storage.notFixedTypeVariables.mapValues { it.value.rawConstraintsCount },
            beforeConstraintsFromAllForks = storage.constraintsFromAllForkPoints.size,
            beforeHasContradictionInForkPointsCache = hasContradictionInForkPointsCache,
        ).also {
            state = State.TRANSACTION
        }
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

        doAddOtherSystem(otherSystem, mergeMode = false)
    }

    /**
     * 以合并模式加入另一个约束系统，重复约束和错误会按身份去重。
     */
    @UnstableSystemMergeMode
    override fun mergeOtherSystem(otherSystem: ConstraintStorage) {
        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(otherSystem, isAddingOuter = false)

        doAddOtherSystem(otherSystem, mergeMode = true)
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

        // Clean all existing data
        // NB: `postponedTypeVariables` is always empty in K2/PCLA, thus no need to clear it
        notFixedTypeVariables.clear()
        typeVariableDependencies.clear()
        storage.initialConstraints.clear()
        storage.errors.clear()
        storage.constraintsFromAllForkPoints.clear()

        // There's no need to clean `allTypeVariables` as `otherSystem.allTypeVariables` is expected to be a superset of what we've got
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            check(otherSystem.allTypeVariables.keys.containsAll(storage.allTypeVariables.keys))
        }

        doAddOtherSystem(otherSystem, mergeMode = false)
    }

    /**
     * 按追加或合并模式把 [otherSystem] 的内部状态并入当前存储。
     */
    private fun doAddOtherSystem(otherSystem: ConstraintStorage, mergeMode: Boolean) {
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
                notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, constraints)
            } else {
                val previous = notFixedTypeVariables[variable]
                if (previous != null) {
                    @OptIn(UnstableSystemMergeMode::class)
                    notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, previous, constraints)
                } else {
                    notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, constraints)
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
    }

    // ConstraintInjector.Context, FixationOrderCalculator.Context
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
        val allForkPointsData = constraintsFromAllForkPoints.toList()
        constraintsFromAllForkPoints.clear()

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
        val isSuccessful = forkPointData.any { constraintSetForForkBranch ->
            runTransaction {
                applyForkPointBranch(constraintSetForForkBranch, position)

                !storage.hasContradiction
            }
        }

        if (!isSuccessful) {
            applyForkPointBranch(forkPointData.first(), position)
        }

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
    ) = with(utilContext) {
        checkState(State.BUILDING, State.COMPLETION)

        checkInferredEmptyIntersection(variable, resultType)

        constraintInjector.addInitialEqualityConstraint(variable.defaultType(), resultType, position)

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
        for ((_, variableWithConstraints) in storage.notFixedTypeVariables) {
            variableWithConstraints.removeConstraints { constraint ->
                constraint.type.contains { it is StubTypeMarker && it.getOriginalTypeVariable() in postponedTypeVariables }
            }
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
