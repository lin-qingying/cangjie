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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.ForkPointBranchDescription
import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.extractAllContainingTypeVariables
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind.*
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.utils.SmartList
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.popLast
import kotlin.math.max

/**
 * 约束注入器。
 *
 * 该组件负责把调用解析阶段产生的初始子类型/等价约束注入约束系统，并驱动类型检查器和
 * [ConstraintIncorporator] 生成派生约束、fork point 约束以及相关错误。
 */
class ConstraintInjector(
    /**
     * 负责根据新增约束执行 incorporation 的组件。
     */
    val constraintIncorporator: ConstraintIncorporator,

    /**
     * 约束处理过程中用于限制内部类型形态的类型近似器。
     */
    val typeApproximator: AbstractTypeApproximator,

    /**
     * 当前语言版本设置。
     */
    private val languageVersionSettings: LanguageVersionSettings,
    inferenceLoggerParameter: InferenceLogger? = null,
) {
    /**
     * 实际使用的推断日志器；Dummy 日志器会被视为无日志。
     */
    private val inferenceLogger = inferenceLoggerParameter.takeIf { it !is InferenceLogger.Dummy }

    /**
     * incorporation 期间允许派生类型深度超过初始约束最大深度的增量。
     */
    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 1

    /**
     * 是否使用初始约束最大类型深度限制 incorporation 产生的约束。
     */
    private val useMaxTypeDepthFromInitialConstraints: Boolean = true

    /**
     * 约束注入器操作当前约束系统所需的上下文接口。
     */
    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemMarker {
        /**
         * 当前系统注册的所有类型变量。
         */
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /**
         * 初始约束中允许参与 incorporation 的最大类型深度。
         */
        var maxTypeDepthFromInitialConstraints: Int

        /**
         * 尚未固定的类型变量及其可变约束集合。
         */
        val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>

        /**
         * 已固定类型变量到结果类型的映射。
         */
        val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker>

        /**
         * 所有 fork point 分支收集到的约束。
         */
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>

        /**
         * @see org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.typeVariableDependencies
         */
        val typeVariableDependencies: Map<TypeConstructorMarker, Set<TypeConstructorMarker>>

        /**
         * 当前约束系统是否处于 completion 阶段。
         */
        val atCompletionState: Boolean

        /**
         * 记录一条初始约束。
         */
        fun addInitialConstraint(initialConstraint: InitialConstraint)

        /**
         * 记录一条约束系统错误。
         */
        fun addError(error: ConstraintSystemError)

        /**
         * 立即解析当前收集到的 fork point 约束。
         */
        fun resolveForkPointsConstraints()

        /**
         * 通知约束系统新增约束或 fork point，以便清理缓存状态。
         */
        fun onNewConstraintOrForkPoint()

        /**
         * 记录 [constraintOwner] 的约束类型中引用了 [referencedVariable]。
         */
        fun recordTypeVariableReferenceInConstraint(
            constraintOwner: TypeConstructorMarker,
            referencedVariable: TypeConstructorMarker,
        )
    }

    /**
     * 添加初始子类型约束 [lowerType] <: [upperType] 并执行 incorporation。
     */
    context(c: Context)
    fun addInitialSubtypeConstraint(
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        position: ConstraintPosition,
    ) {
        val initialConstraint = InitialConstraint(lowerType, upperType, UPPER, position).also { c.addInitialConstraint(it) }
        inferenceLogger?.logInitial(initialConstraint, c)

        updateAllowedTypeDepth(lowerType)
        updateAllowedTypeDepth(upperType)

        inferenceLogger?.withOrigin(initialConstraint) {
            with(TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))) {
                addSubTypeConstraintAndIncorporateIt(lowerType, upperType)
            }
        } ?: with(TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))) {
            addSubTypeConstraintAndIncorporateIt(lowerType, upperType)
        }
    }

    /**
     * 通过双向子类型约束注入初始等价约束。
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun addInitialEqualityConstraintThroughSubtyping(a: CangJieTypeMarker, b: CangJieTypeMarker) {
        updateAllowedTypeDepth(a)
        updateAllowedTypeDepth(b)
        addSubTypeConstraintAndIncorporateIt(a, b)
        addSubTypeConstraintAndIncorporateIt(b, a)
    }

    /**
     * 添加初始等价约束 [a] == [b] 并执行 incorporation。
     */
    context(c: Context)
    fun addInitialEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition) {
        val (typeVariable, equalType) = when {
            a.typeConstructor() is TypeVariableTypeConstructorMarker -> a to b
            b.typeConstructor() is TypeVariableTypeConstructorMarker -> b to a
            else -> return
        }
        val initialConstraint = InitialConstraint(typeVariable, equalType, EQUALITY, position).also { c.addInitialConstraint(it) }
        inferenceLogger?.logInitial(initialConstraint, c)

        with(TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))) {
            if (!typeVariable.isRigidType()) {
                inferenceLogger?.withOrigin(initialConstraint) {
                    addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType)
                } ?: addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType)
                return
            }

            updateAllowedTypeDepth(equalType)
            inferenceLogger?.withOrigin(initialConstraint) {
                addEqualityConstraintAndIncorporateIt(typeVariable, equalType)
            } ?: addEqualityConstraintAndIncorporateIt(typeVariable, equalType)
        }
    }

    /**
     * 向类型检查器注入子类型关系，并处理产生的新约束。
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun addSubTypeConstraintAndIncorporateIt(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(lowerType, upperType)
        typeCheckerState.runIsSubtypeOf(lowerType, upperType)

        processConstraints()
    }

    /**
     * 向类型检查器注入类型变量等价关系，并处理产生的新约束。
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun addEqualityConstraintAndIncorporateIt(typeVariable: CangJieTypeMarker, equalType: CangJieTypeMarker) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(typeVariable, equalType)
        val typeSystemContext = c as TypeSystemContext
        typeCheckerState.addEqualityConstraint(with(typeSystemContext) { typeVariable.typeConstructor() }, equalType)

        processConstraints()
    }

    /**
     * 处理指定 fork point 分支中收集到的约束集合。
     */
    context(c: Context)
    fun processGivenForkPointBranchConstraints(
        constraintSet: Collection<Pair<TypeVariableMarker, Constraint>>,
        position: IncorporationConstraintPosition,
    ) {
        with(TypeCheckerStateForConstraintInjector(c, position)) {
            processGivenConstraints(constraintSet)
            processConstraintsIgnoringForksData()
        }
    }

    /**
     * 处理类型检查器产生的普通约束和 fork point 约束。
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun processConstraints() {
        processConstraintsIgnoringForksData()
        typeCheckerState.extractForkPointsData()?.let { allForkPointsData ->
            allForkPointsData.mapTo(c.constraintsFromAllForkPoints) { forkPointData ->
                typeCheckerState.position to forkPointData
            }

            c.onNewConstraintOrForkPoint()

            // During completion, we start processing fork constrains immediately
            if (c.atCompletionState) {
                c.resolveForkPointsConstraints()
            }
        }
    }

    /**
     * 只处理普通约束，不提取 fork point 数据。
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun processConstraintsIgnoringForksData() {
        while (typeCheckerState.hasConstraintsToProcess()) {
            processGivenConstraints(typeCheckerState.extractAllConstraints()!!)
        }
    }

    /**
     * 将 [constraintsToProcess] 合并进当前约束系统，并对新增约束执行 incorporation。
     */
    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun processGivenConstraints(constraintsToProcess: Collection<Pair<TypeVariableMarker, Constraint>>) {
        for ((typeVariable, constraint) in constraintsToProcess) {
            if (shouldWeSkipConstraint(typeVariable, constraint)) continue

            val inferenceContext = c as TypeSystemInferenceExtensionContext
            val typeVariableConstructor = with(inferenceContext) { typeVariable.freshTypeConstructor() }
            val constraints =
                c.notFixedTypeVariables[typeVariableConstructor] ?: typeCheckerState.fixedTypeVariable(typeVariable)

            // it is important, that we add constraint here(not inside TypeCheckerContext), because inside incorporation we read constraints
            val (addedOrNonRedundantExistedConstraint, wasAdded) = constraints.addConstraint(constraint, inferenceLogger)
            val positionFrom = constraint.position.from
            val constraintToIncorporate = when {
                wasAdded -> addedOrNonRedundantExistedConstraint
                positionFrom is FixVariableConstraintPosition<*> && positionFrom.variable == typeVariable && constraint.kind == EQUALITY ->
                    addedOrNonRedundantExistedConstraint
                else -> null
            }

            if (wasAdded) {
                inferenceLogger?.log(typeVariable, addedOrNonRedundantExistedConstraint, c)
                c.onNewConstraintOrForkPoint()
                recordReferencesOfOtherTypeVariableInConstraint(constraint, typeVariableConstructor)
            }

            if (constraintToIncorporate != null) {
                constraintIncorporator.incorporate(typeVariable, constraintToIncorporate)
            }
        }
    }

    /**
     * 记录约束类型中引用的其他类型变量，用于依赖索引维护。
     */
    context(c: Context)
    private fun recordReferencesOfOtherTypeVariableInConstraint(
        constraint: Constraint,
        constraintOwnerTypeVariableConstructor: TypeConstructorMarker,
    ) {
        for (referencedTypeVariableConstructor in constraint.type.extractAllContainingTypeVariables()) {
            c.recordTypeVariableReferenceInConstraint(constraintOwnerTypeVariableConstructor, referencedTypeVariableConstructor)
        }
    }

    /**
     * 根据 [type] 更新初始约束允许的最大类型深度。
     */
    context(c: Context)
    private fun updateAllowedTypeDepth(type: CangJieTypeMarker) {
        if (!useMaxTypeDepthFromInitialConstraints) return
        c.maxTypeDepthFromInitialConstraints = max(c.maxTypeDepthFromInitialConstraints, with(c) { type.typeDepth() })
    }

    /**
     * 判断当前派生约束是否可以跳过。
     */
    context(c: Context)
    private fun shouldWeSkipConstraint(typeVariable: TypeVariableMarker, constraint: Constraint): Boolean {
        if (constraint.kind == EQUALITY)
            return false

        val constraintType = constraint.type

        if (constraintType.typeConstructor() == typeVariable.freshTypeConstructor()) {
            return true
        }

        return false
    }

    /**
     * 判断当前类型深度是否仍处于 incorporation 允许范围内。
     */
    context(c: Context)
    private fun CangJieTypeMarker.isAllowedType(): Boolean {
        return with(c) { typeDepth() } <= c.maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION
    }

    /**
     * 约束注入阶段使用的类型检查状态。
     *
     * 该状态接收 [AbstractTypeChecker] 产生的新约束，暂存 fork point 分支数据，并实现
     * [ConstraintIncorporator.Context] 供 incorporation 继续派生约束。
     */
    private inner class TypeCheckerStateForConstraintInjector(
        /**
         * 底层类型检查状态。
         */
        baseState: TypeCheckerState,

        /**
         * 当前约束注入上下文。
         */
        val c: Context,

        /**
         * 当前 incorporation 位置。
         */
        val position: IncorporationConstraintPosition
    ) : TypeCheckerStateForConstraintSystem(
        c,
        baseState.cangjieTypePreparator,
        baseState.cangjieTypeRefiner,
    ), ConstraintIncorporator.Context, TypeSystemInferenceExtensionContext by c {
        constructor(c: Context, position: IncorporationConstraintPosition) : this(
            c.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true),
            c,
            position
        )

        // We use `var` intentionally to avoid extra allocations as this property is quite "hot"
        /**
         * 当前批次收集到的普通新增约束。
         */
        private var possibleNewConstraints: MutableList<Pair<TypeVariableMarker, Constraint>>? = null

        /**
         * 当前批次收集到的 fork point 数据。
         */
        private var forkPointsData: MutableList<ForkPointData>? = null

        /**
         * 当前 fork point 中各分支约束集合的栈。
         */
        private var stackForConstraintsSetsFromCurrentForkPoint: Stack<MutableList<ForkPointBranchDescription>>? = null

        /**
         * 当前 fork point 分支内约束集合的栈。
         */
        private var stackForConstraintSetFromCurrentForkPointBranch: Stack<MutableList<Pair<TypeVariableMarker, Constraint>>>? = null

        /**
         * 当前语言版本设置。
         */
        override val languageVersionSettings: LanguageVersionSettings
            get() = this@ConstraintInjector.languageVersionSettings

        /**
         * 是否允许把类型检查过程中的分支记录为 fork point。
         */
        private val allowForking: Boolean
            get() = constraintIncorporator.utilContext.isForcedAllowForkingInferenceSystem

        /**
         * 用于调试输出的基础下界类型。
         */
        private var baseLowerType = position.initialConstraint.a

        /**
         * 用于调试输出的基础上界类型。
         */
        private var baseUpperType = position.initialConstraint.b

        /**
         * 当前是否正在 incorporation 声明上界派生约束。
         */
        private var isIncorporatingConstraintFromDeclaredUpperBound = false

        /**
         * 当前是否正在 incorporation NoInfer 派生约束。
         */
        private var isIncorporatingConstraintFromNoInfer = false

        /**
         * 当前派生约束继承的来源类型变量集合。
         */
        private var currentDerivedFromSet: Set<TypeVariableMarker> = emptySet()

        /**
         * 取出并清空当前批次普通新增约束。
         */
        fun extractAllConstraints() = possibleNewConstraints.also { possibleNewConstraints = null }

        /**
         * 取出并清空当前批次 fork point 数据。
         */
        fun extractForkPointsData() = forkPointsData.also { forkPointsData = null }

        /**
         * 添加类型检查或 incorporation 产生的新约束。
         */
        fun addPossibleNewConstraint(variable: TypeVariableMarker, constraint: Constraint) {
            val constraintsSetsFromCurrentFork = stackForConstraintsSetsFromCurrentForkPoint?.lastOrNull()
            if (constraintsSetsFromCurrentFork != null) {
                val currentConstraintSetForForkPointBranch = stackForConstraintSetFromCurrentForkPointBranch?.lastOrNull()
                require(currentConstraintSetForForkPointBranch != null) { "Constraint has been added not under fork {...} call " }
                currentConstraintSetForForkPointBranch.add(variable to constraint)
                return
            }

            if (possibleNewConstraints == null) {
                possibleNewConstraints = SmartList()
            }
            possibleNewConstraints!!.add(variable to constraint)
        }

        /**
         * 执行一个可分叉的类型检查点，并按分支数量决定是否生成 fork point 数据。
         */
        override fun runForkingPoint(block: ForkPointContext.() -> Unit): Boolean {
            if (!allowForking) {
                return super.runForkingPoint(block)
            }

            if (stackForConstraintsSetsFromCurrentForkPoint == null) {
                stackForConstraintsSetsFromCurrentForkPoint = SmartList()
            }

            stackForConstraintsSetsFromCurrentForkPoint!!.add(SmartList())
            val isThereSuccessfulFork = with(MyForkCreationContext()) {
                block()
                anyForkSuccessful
            }

            val constraintSets = stackForConstraintsSetsFromCurrentForkPoint?.popLast()

            when {
                // Just an optimization
                constraintSets.isNullOrEmpty() -> return isThereSuccessfulFork
                constraintSets.size > 1 -> {
                    if (forkPointsData == null) {
                        forkPointsData = SmartList()
                    }
                    forkPointsData!!.addIfNotNull(
                        constraintSets
                    )
                    return isThereSuccessfulFork
                }
                else -> {
                    // The emptiness case has been already handled above
                    with(c) {
                        processGivenForkPointBranchConstraints(
                            constraintSets.single(),
                            position,
                        )
                    }
                }
            }

            return isThereSuccessfulFork
        }

        /**
         * fork point 创建上下文。
         */
        private inner class MyForkCreationContext : ForkPointContext {
            /**
             * 当前 fork point 中是否至少有一个分支成功。
             */
            var anyForkSuccessful = false

            /**
             * 执行单个 fork 分支并记录该分支新增的约束集合。
             */
            override fun fork(block: () -> Boolean) {
                if (stackForConstraintSetFromCurrentForkPointBranch == null) {
                    stackForConstraintSetFromCurrentForkPointBranch = SmartList()
                }

                stackForConstraintSetFromCurrentForkPointBranch!!.add(SmartList())

                block().also { anyForkSuccessful = anyForkSuccessful || it }

                stackForConstraintsSetsFromCurrentForkPoint!!.last()
                    .addIfNotNull(
                        stackForConstraintSetFromCurrentForkPointBranch?.popLast()?.takeIf { it.isNotEmpty() }?.toSet()
                    )
            }
        }

        /**
         * 当前状态是否仍有普通约束需要处理。
         */
        fun hasConstraintsToProcess() = possibleNewConstraints != null

        /**
         * 设置基础约束类型，用于后续错误调试信息。
         */
        fun setConstrainingTypesToPrintDebugInfo(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker) {
            baseLowerType = lowerType
            baseUpperType = upperType
        }

        /**
         * 执行子类型检查，并在失败时向约束系统记录约束错误。
         */
        fun runIsSubtypeOf(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
        ) {
            val typeCheckerState = this@TypeCheckerStateForConstraintInjector as TypeCheckerState
            val checksDeclaredUpperBound =
                isIncorporatingConstraintFromDeclaredUpperBound ||
                    position.from is DeclaredUpperBoundConstraintPosition<*>
            fun isSubtypeOf(upperType: CangJieTypeMarker) = if (checksDeclaredUpperBound) {
                AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                    typeCheckerState,
                    lowerType,
                    upperType,
                )
            } else {
                AbstractTypeChecker.isSubtypeOf(typeCheckerState, lowerType, upperType)
            }

            if (!isSubtypeOf(upperType)) {

                c.addError(ConstraintError(lowerType, upperType, position))
            }
        }

        // from AbstractTypeCheckerContextForConstraintSystem
        /**
         * 判断 [type] 是否属于当前约束系统的类型变量。
         */
        override fun isMyTypeVariable(type: RigidTypeMarker): Boolean =
            c.allTypeVariables.containsKey(type.typeConstructor().unwrapStubTypeVariableConstructor())

        /**
         * 为 [typeVariable] 添加上界约束。
         */
        override fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: CangJieTypeMarker, isNoInfer: Boolean) =
            addConstraint(
                typeVariable, superType, UPPER,
                isNoInfer = isNoInfer
            )

        /**
         * 为 [typeVariable] 添加下界约束。
         */
        override fun addLowerConstraint(
            typeVariable: TypeConstructorMarker,
            subType: CangJieTypeMarker,
            isFromNullabilityConstraint: Boolean,
            isNoInfer: Boolean,
        ) = addConstraint(typeVariable, subType, LOWER, isNoInfer)

        /**
         * 为 [typeVariable] 添加等价约束。
         */
        override fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: CangJieTypeMarker) =
            addConstraint(
                typeVariable, type, EQUALITY,
                isNoInfer = false
            )

        /**
         * 将类型检查器报告的约束转换为约束系统内部约束。
         */
        private fun addConstraint(
            typeVariableConstructor: TypeConstructorMarker,
            type: CangJieTypeMarker,
            kind: ConstraintKind,
            isNoInfer: Boolean,
        ) {
            val typeVariable = c.allTypeVariables[typeVariableConstructor.unwrapStubTypeVariableConstructor()]
                ?: error("Should by type variableConstructor: $typeVariableConstructor. ${c.allTypeVariables.values}")

            addNewIncorporatedConstraint(
                typeVariable,
                type,
                ConstraintContext(
                    kind = kind,
                    derivedFrom = currentDerivedFromSet,
                    isNoInfer = isNoInfer
                )
            )
        }

        // from ConstraintIncorporator.Context
        /**
         * 处理 incorporation 新产生的初始子类型约束。
         */
        override fun processNewInitialConstraintFromIncorporation(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            newDerivedFrom: Set<TypeVariableMarker>,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean
        ) = with(c) {
            // Avoid checking trivial incorporated constraints
            if (lowerType == upperType) return
            if (!useMaxTypeDepthFromInitialConstraints || (lowerType.isAllowedType() && upperType.isAllowedType())) {
                withNewConfigurationForIncorporationConstraints(
                    newDerivedFromSet = newDerivedFrom,
                    isFromDeclaredUpperBound = isFromDeclaredUpperBound,
                    isNoInfer = isNoInfer,
                ) {
                    runIsSubtypeOf(lowerType, upperType, )
                }
            }
        }

        /**
         * 在新的派生约束上下文中执行 [b]，结束后恢复状态。
         */
        private inline fun withNewConfigurationForIncorporationConstraints(
            newDerivedFromSet: Set<TypeVariableMarker>,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean,
            b: () -> Unit,
        ) {
            // No immediate recursive incorporation should happen, so `currentDerivedFromSet` would be reset at "finally"
            check(currentDerivedFromSet.isEmpty())

            try {
                currentDerivedFromSet = newDerivedFromSet
                isIncorporatingConstraintFromDeclaredUpperBound = isFromDeclaredUpperBound
                isIncorporatingConstraintFromNoInfer = isNoInfer
                b()
            } finally {
                // NB: `emptySet()` returns a singleton, so no excessive memory here
                currentDerivedFromSet = emptySet()
                isIncorporatingConstraintFromDeclaredUpperBound = false
                isIncorporatingConstraintFromNoInfer = false
            }
        }

        /**
         * 添加 incorporation 新产生的类型变量约束。
         */
        override fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: CangJieTypeMarker,
            constraintContext: ConstraintContext
        ) {
            val (kind, derivedFrom, inputTypePosition, isNoInfer) = constraintContext

            var targetType = type
            if (targetType.isUninferredParameter()) {
                // there already should be an error, so there is no point in reporting one more
                return
            }

            if (targetType.isError()) {
                c.addError(ConstrainingTypeIsError(typeVariable, targetType, position))
                return
            }

            val position = if (isIncorporatingConstraintFromDeclaredUpperBound) position.copy(isFromDeclaredUpperBound = true) else position

            val newConstraint = Constraint(
                kind, targetType, position,
                derivedFrom = derivedFrom,
                isNoInfer = isNoInfer || isIncorporatingConstraintFromNoInfer,
                inputTypePositionBeforeIncorporation = inputTypePosition,
            )

            addPossibleNewConstraint(typeVariable, newConstraint)
        }

        /**
         * 当前所有未固定变量的约束视图。
         */
        override val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>
            get() = c.notFixedTypeVariables.values

        /**
         * 获取约束中可能包含指定类型变量的变量约束集合。
         */
        override fun getVariablesWithConstraintsContainingGivenTypeVariable(
            variableConstructorMarker: TypeConstructorMarker
        ): Collection<VariableWithConstraints> =
            c.typeVariableDependencies[variableConstructorMarker]?.mapNotNull { c.notFixedTypeVariables[it] }
                ?: emptyList()

        /**
         * 按类型构造器查找当前约束系统中的类型变量。
         */
        override fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker? {
            val typeVariable = c.allTypeVariables[typeConstructor]
            if (typeVariable != null && !c.notFixedTypeVariables.containsKey(typeConstructor)) {
                fixedTypeVariable(typeVariable)
            }
            return typeVariable
        }

        /**
         * 获取指定类型变量当前的约束列表。
         */
        override fun getConstraintsForVariable(typeVariable: TypeVariableMarker) =
            c.notFixedTypeVariables[typeVariable.freshTypeConstructor()]?.constraints
                ?: fixedTypeVariable(typeVariable)

        /**
         * 在类型检查过程中遇到已固定变量时抛出内部错误。
         */
        fun fixedTypeVariable(variable: TypeVariableMarker): Nothing {
            error(
                "Type variable $variable should not be fixed!\n" +
                        renderBaseConstraint()
            )
        }

        /**
         * 渲染当前基础约束的调试文本。
         */
        private fun renderBaseConstraint() = "Base constraint: $baseLowerType <: $baseUpperType from position: $position"
    }
}

/**
 * incorporation 生成新约束时携带的上下文信息。
 */
data class ConstraintContext(
    /**
     * 新约束的方向。
     */
    val kind: ConstraintKind,

    /**
     * 新约束来源的类型变量集合。
     */
    val derivedFrom: Set<TypeVariableMarker>,

    /**
     * incorporation 前保留的 OnlyInputType 位置。
     */
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,

    /**
     * 新约束是否携带 NoInfer 语义。
     */
    val isNoInfer: Boolean,
)

/**
 * 用可变列表表示的轻量栈。
 */
private typealias Stack<E> = MutableList<E>
