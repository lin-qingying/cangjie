/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode.PARTIAL
import org.cangnova.cangjie.resolve.calls.inference.components.InferenceLogger.FixationLogRecord
import org.cangnova.cangjie.resolve.calls.inference.components.InferenceLogger.FixationLogVariableInfo
import org.cangnova.cangjie.resolve.calls.inference.components.VariableFixationFinder.Context
import org.cangnova.cangjie.resolve.calls.inference.components.VariableFixationFinder.VariableForFixation
import org.cangnova.cangjie.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.cangnova.cangjie.resolve.calls.inference.isRecursiveTypeParameter
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.*
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * 类型变量固定候选查找器。
 *
 * 该组件根据未固定变量的依赖关系、postponed 参数状态、完成模式和 readiness 策略，
 * 选出当前最适合固定的类型变量。
 */
class VariableFixationFinder(
    /**
     * 当前语言版本设置，传递给 readiness 策略用于兼容性判断。
     */
    private val languageVersionSettings: LanguageVersionSettings,

    /**
     * 具体的变量就绪度计算策略。
     */
    private val variableReadinessCalculator: AbstractVariableReadinessCalculator<*>,
) {

    /**
     * 变量固定查找所需的约束系统上下文。
     */
    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemMarker {
        /**
         * 当前仍未固定的类型变量及其约束。
         */
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

        /**
         * 已经固定的类型变量及其结果类型。
         */
        val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>

        /**
         * 尚需延迟处理的类型变量。
         */
        val postponedTypeVariables: List<TypeVariableMarker>

        /**
         * 所有 fork point 分支产生的约束。
         */
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>

        /**
         * 当前约束系统注册的全部类型变量。
         */
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /**
         * See [org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.outerSystemVariablesPrefixSize]
         */
        /**
         * 外部约束系统变量在 [allTypeVariables] 中占用的前缀长度。
         */
        val outerSystemVariablesPrefixSize: Int

        /**
         * 外部约束系统变量集合；当前系统未嵌套在外部系统中时为 `null`。
         */
        val outerTypeVariables: Set<TypeConstructorMarker>?
            get() =
                when {
                    outerSystemVariablesPrefixSize > 0 -> allTypeVariables.keys.take(outerSystemVariablesPrefixSize).toSet()
                    else -> null
                }

        /**
         * If not null, that property means that we should assume temporary them all as proper types when fixating some variables.
         *
         * By default, if that property is null, we assume all `allTypeVariables` as not proper.
         *
         * Currently, that is only used for `provideDelegate` resolution, see
         * [org.cangnova.cangjie.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
         */
        /**
         * 固定某些变量时临时视为 proper type 的类型变量集合。
         */
        val typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>?

    }

    /**
     * 待固定类型变量的候选描述。
     */
    class VariableForFixation(
        /**
         * 候选类型变量的构造器。
         */
        val variable: TypeConstructorMarker,

        /**
         * 候选变量是否拥有可用于固定的 proper 约束。
         */
        private val hasProperConstraint: Boolean,

        /**
         * 候选变量是否依赖外层约束系统变量。
         */
        private val hasDependencyOnOuterTypeVariable: Boolean = false,
    ) {
        /**
         * 候选变量当前是否可以直接固定。
         */
        val isReady: Boolean get() = hasProperConstraint && !hasDependencyOnOuterTypeVariable
    }

    /**
     * 从 [allTypeVariables] 中查找第一个可用于固定的变量候选。
     */
    context(c: Context)
    fun findFirstVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: CangJieTypeMarker,
    ): VariableForFixation? =
        findTypeVariableForFixation(allTypeVariables, postponedKtPrimitives, completionMode, topLevelType)?.also { variable ->
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                require(!variable.isReady || c.notFixedTypeVariables[variable.variable]?.constraints?.any { !it.isNoInfer } == true)
            }
        }

    /**
     * 判断 [typeVariable] 是否拥有可用于固定的 proper 约束。
     */
    context(c: Context)
    fun typeVariableHasProperConstraint(typeVariable: TypeConstructorMarker): Boolean {
        val dependencyProvider = TypeVariableDependencyInformationProvider(
            c.notFixedTypeVariables, emptyList(), topLevelType = null, c,
            languageVersionSettings,
        )

        return variableReadinessCalculator.typeVariableHasProperConstraint(typeVariable, dependencyProvider)
    }

    /**
     * 构建依赖信息并委托 readiness 策略选择最佳固定候选。
     */
    context(c: Context)
    private fun findTypeVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: CangJieTypeMarker,
    ): VariableForFixation? {
        if (allTypeVariables.isEmpty()) return null

        val dependencyProvider = TypeVariableDependencyInformationProvider(
            c.notFixedTypeVariables, postponedArguments, topLevelType.takeIf { completionMode == PARTIAL }, c,
            languageVersionSettings,
        )

        val candidate = variableReadinessCalculator.chooseBestTypeVariableCandidateWithLogging(allTypeVariables, dependencyProvider)
            ?: return null
        return variableReadinessCalculator.prepareVariableForFixation(candidate, dependencyProvider)
    }
}

/**
 * 类型变量就绪度计算器基类。
 *
 * 子类通过具体 [Readiness] 排序规则表达不同推断阶段对变量固定顺序的偏好。
 */
abstract class AbstractVariableReadinessCalculator<Readiness : Comparable<Readiness>>(
    /**
     * 判断约束是否平凡或不值得推动固定的 oracle。
     */
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,

    /**
     * 当前语言版本设置。
     */
    private val languageVersionSettings: LanguageVersionSettings,

    /**
     * 可选推断日志器，用于记录变量 readiness 与最终选择。
     */
    private val inferenceLogger: InferenceLogger? = null,
) {

    /**
     * 计算当前类型变量在 [dependencyProvider] 下的就绪度。
     */
    context(c: Context)
    abstract fun TypeConstructorMarker.getReadiness(dependencyProvider: TypeVariableDependencyInformationProvider): Readiness

    /**
     * 将已选择的 [candidate] 转换为可固定变量描述；不适合固定时返回 `null`。
     */
    context(c: Context)
    abstract fun prepareVariableForFixation(
        candidate: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): VariableForFixation?

    /**
     * 判断 [typeVariable] 是否有 proper 约束。
     */
    context(c: Context)
    abstract fun typeVariableHasProperConstraint(
        typeVariable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): Boolean

    /**
     * 2.2 版本固定增强是否启用。
     */
    protected val fixationEnhancementsIn22: Boolean
        get() = true

    /**
     * 判断当前变量是否直接约束到仍未固定的相关变量。
     */
    context(c: Context)
    protected fun TypeConstructorMarker.hasDirectConstraintToNotFixedRelevantVariable(): Boolean {
        return c.notFixedTypeVariables[this]?.constraints?.any { it.type.isNotFixedRelevantVariable() } == true
    }

    /**
     * 判断当前变量是否在尚未处理的 fork point 约束中出现。
     */
    context(c: Context)
    protected fun TypeConstructorMarker.hasUnprocessedConstraintsInForks(): Boolean {
        if (c.constraintsFromAllForkPoints.isEmpty()) return false

        for ((_, forkPointData) in c.constraintsFromAllForkPoints) {
            for (constraints in forkPointData) {
                for ((typeVariableFromConstraint, constraint) in constraints) {
                    if (typeVariableFromConstraint.freshTypeConstructor() == this) return true
                    if (constraint.type.containsTypeVariable(this)) return true
                }
            }
        }

        return false
    }

    /**
     * 判断当前变量的所有约束是否都是平凡约束或 non-proper 约束。
     */
    context(c: Context)
    protected fun TypeConstructorMarker.allConstraintsTrivialOrNonProper(): Boolean {
        return c.notFixedTypeVariables[this]?.constraints?.all { constraint ->
            trivialConstraintTypeInferenceOracle.isNotInterestingConstraint(constraint) || !constraint.isProperArgumentConstraint()
        } ?: false
    }

    /**
     * 判断当前变量是否只有由声明上界 incorporation 产生的 proper 约束。
     */
    context(c: Context)
    protected fun TypeConstructorMarker.hasOnlyIncorporatedConstraintsFromDeclaredUpperBound(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false

        fun Constraint.isTrivial() = kind == ConstraintKind.LOWER && type.isNothing()
                || kind == ConstraintKind.UPPER && type.typeConstructor().isAnyConstructor()

        return constraints.filter { it.isProperArgumentConstraint() && !it.isTrivial() }.all { it.position.isFromDeclaredUpperBound }
    }

    /**
     * 选择最佳类型变量候选，并在存在日志器时记录所有候选的 readiness。
     */
    context(c: Context)
    fun chooseBestTypeVariableCandidateWithLogging(
        allTypeVariables: List<TypeConstructorMarker>,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): TypeConstructorMarker? {
        if (inferenceLogger == null) {
            return allTypeVariables.maxByOrNull { it.getReadiness(dependencyProvider) }
        }

        val readinessPerVariable = allTypeVariables.associateWith {
            FixationLogVariableInfo(
                it.getReadiness(dependencyProvider),
                c.notFixedTypeVariables[it]?.constraints.orEmpty()
            )
        }
        val chosen = readinessPerVariable.maxByOrNull { (_, value) -> value.readiness.toString() }?.key
        val newRecord = FixationLogRecord(
            readinessPerVariable.mapKeys { (key, _) -> c.allTypeVariables[key]!! }, c.allTypeVariables[chosen]
        )

        inferenceLogger.logReadiness(newRecord, c)
        return chosen
    }

    /**
     * 判断当前变量是否依赖其他未固定类型变量。
     */
    context(c: Context)
    protected fun TypeConstructorMarker.hasDependencyToOtherTypeVariables(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false
        return constraints.any { it.hasDependencyToOtherTypeVariable(this) }
    }

    /**
     * 判断该约束类型是否引用了所属变量之外的未固定变量。
     */
    context(c: Context)
    private fun Constraint.hasDependencyToOtherTypeVariable(ownerTypeVariable: TypeConstructorMarker): Boolean {
        return type.argumentsCount() != 0 &&
                type.contains { it.typeConstructor() != ownerTypeVariable && c.notFixedTypeVariables.containsKey(it.typeConstructor()) }
    }

    // IltRelatedFlags can't be a combination of 1/0, as any non-ILT equality proper constraint is also a non-ILT proper constraint
    /**
     * 整型字面量相关约束的分类标志。
     */
    protected data class IltRelatedFlags(
        /**
         * @return true if a considered type variable has a proper EQUALS constraint T = SomeType, and SomeType is not an ILT-type
         */
        val hasProperNonIltEqualityConstraint: Boolean,
        /**
         * @return true if a considered type variable has a proper constraint T vs SomeType, and SomeType is not an ILT-type
         */
        val hasProperNonIltConstraint: Boolean,
    )

    /**
     * 计算当前变量的非 ILT proper 约束和非 ILT 等价约束标志。
     */
    context(c: Context)
    protected fun TypeConstructorMarker.computeIltConstraintsRelatedFlags(): IltRelatedFlags {
        val constraints = c.notFixedTypeVariables[this]?.constraints
        if (!fixationEnhancementsIn22 || constraints == null) return IltRelatedFlags(false, false)

        var hasProperNonIltEqualityConstraint = false
        var hasProperNonIltConstraint = false

        for (it in constraints) {
            val isProper = it.isProperArgumentConstraint()
            val containsIlt = it.type.contains { it.typeConstructor().isIntegerLiteralTypeConstructor() }
            val isProperNonIlt = isProper && !containsIlt

            hasProperNonIltEqualityConstraint = hasProperNonIltEqualityConstraint || isProperNonIlt && it.kind == ConstraintKind.EQUALITY
            hasProperNonIltConstraint = hasProperNonIltConstraint || isProperNonIlt
        }

        return IltRelatedFlags(hasProperNonIltEqualityConstraint, hasProperNonIltConstraint)
    }

    /**
     * 判断当前变量是否存在可用于固定的 proper 参数约束。
     */
    context(c: Context)
    protected fun TypeConstructorMarker.hasProperArgumentConstraints(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints ?: return false
        val anyProperConstraint = constraints.any { it.isProperArgumentConstraint() }
        if (!anyProperConstraint) return false

        // temporary hack to fail calls which contain callable references resolved though OI with uninferred type parameters
        val areThereConstraintsWithUninferredTypeParameter = constraints.any { c -> c.type.contains { it.isUninferredParameter() } }
        if (areThereConstraintsWithUninferredTypeParameter) return false

        // The code below is only relevant to [FirInferenceSession.semiFixTypeVariablesAllowingFixationToOtherOnes] case,
        // which is expected to be used only for semi-fixation of input types for input types for OverloadResolutionByLambdaReturnType.
        if (!c.allowSemiFixationToOtherTypeVariables) return true

        val properConstraints = constraints.filter { it.isProperArgumentConstraint() }
        if (properConstraints.any { it.kind != ConstraintKind.LOWER }) return true

        // NB: All proper constraints are LOWER here.
        // As a resulting type for such a type variable is the common supertype of all lower constraints, which is undefined
        // for a case when all the constraints are type variables _and_ there are more than one of them.
        // For details, see [CommonSuperTypeCalculator.commonSuperTypeForNotNullTypes]
        val commonSupertypeIsUndefined = properConstraints.size > 1 && properConstraints.all {
            it.type.typeConstructor() in c.notFixedTypeVariables
        }

        return !commonSupertypeIsUndefined
    }

    /**
     * 判断该约束是否是可用于参数方向固定的 proper 约束。
     */
    context(c: Context)
    protected fun Constraint.isProperArgumentConstraint() =
        type.isProperType()
                && position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition<*>
                && !isNoInfer

    /**
     * 判断当前类型是否可作为固定阶段的 proper type。
     */
    context(c: Context)
    private fun CangJieTypeMarker.isProperType(): Boolean =
        isProperTypeForFixation(
            c.notFixedTypeVariables.keys
        ) { t -> !t.contains { it.isNotFixedRelevantVariable() } }

    /**
     * 判断当前类型是否是仍未固定且未被临时视为 proper 的相关变量。
     */
    context(c: Context)
    private fun CangJieTypeMarker.isNotFixedRelevantVariable(): Boolean {
        val key = typeConstructor()
        if (!c.notFixedTypeVariables.containsKey(key)) return false
        if (c.typeVariablesThatAreCountedAsProperTypes?.contains(key) == true) return false
        return true
    }


    /**
     * 判断该约束是否为声明上界中的递归 self type 约束。
     */
    context(c: Context)
    private fun Constraint.isProperSelfTypeConstraint(ownerTypeVariable: TypeConstructorMarker): Boolean {
        val typeConstructor = type.typeConstructor()
        return position.from is DeclaredUpperBoundConstraintPosition<*>
                && (typeConstructor.hasRecursiveTypeParametersWithGivenSelfType() || typeConstructor.isRecursiveTypeParameter())
                && !hasDependencyToOtherTypeVariable(ownerTypeVariable)
    }

    /**
     * 判断当前变量的所有 proper 约束是否都只来自递归 self type 上界。
     */
    context(c: Context)
    protected fun TypeConstructorMarker.areAllProperConstraintsSelfTypeBased(): Boolean {
        val constraints = c.notFixedTypeVariables[this]?.constraints?.takeIf { it.isNotEmpty() } ?: return false

        var hasSelfTypeConstraint = false
        var hasOtherProperConstraint = false

        for (constraint in constraints) {
            if (constraint.isProperSelfTypeConstraint(this)) {
                hasSelfTypeConstraint = true
            }
            if (constraint.isProperArgumentConstraint()) {
                hasOtherProperConstraint = true
            }
            if (hasSelfTypeConstraint && hasOtherProperConstraint) break
        }

        return hasSelfTypeConstraint && !hasOtherProperConstraint
    }
}

/**
 * Returns `false` for fixed type variables types even if `isProper(type) == true`
 * Thus allowing only non-TVs types to be used for fixation on top level.
 * While this limitation is important, it doesn't really limit final results because when we have a constraint like T <: E or E <: T
 * and we're going to fix T into E, we assume that if E has some other constraints, they are being incorporated to T, so we would choose
 * them instead of E itself.
 */
context(c: TypeSystemInferenceExtensionContext)
inline fun CangJieTypeMarker.isProperTypeForFixation(
    notFixedTypeVariables: Set<TypeConstructorMarker>,
    isProper: (CangJieTypeMarker) -> Boolean
): Boolean {
    // We don't allow fixing T into any top-level TV type, like T := F or T := F & Any
    // Even if F is considered as a proper by `isProper` (e.g., it belongs to an outer CS)
    // But at the same time, we don't forbid fixing into T := MutableList<F>
    // Exception: semi-fixing to other type variables is allowed during overload resolution by lambda return type
    if (!c.allowSemiFixationToOtherTypeVariables && typeConstructor() in notFixedTypeVariables) {
        return false
    }
    return isProper(this)
}

/**
 * 仓颉泛型严格不变，无 CapturedType，直接递归检查类型实参中是否包含类型变量。
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.containsTypeVariable(typeVariable: TypeConstructorMarker): Boolean {
    return contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }
}
