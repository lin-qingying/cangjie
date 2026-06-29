/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemMarker
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.cangnova.cangjie.resolve.calls.inference.components.InferenceLogger
import org.cangnova.cangjie.resolve.calls.inference.extractAllContainingTypeVariables
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.TypeApproximatorCachesPerConfiguration
import org.cangnova.cangjie.utils.SmartList
import org.cangnova.cangjie.utils.trimToSize
import java.util.*

// 将 ConstraintSystemMarker 作为本文件内的 Context 类型别名，简化代码书写
/**
 * 本文件内约束系统上下文的简写。
 */
private typealias Context = ConstraintSystemMarker

/**
 * 持有某个类型变量的所有约束，并支持增删、去重、化简等操作。
 *
 * 构造函数为 private，外部通过三个次构造函数创建实例：
 *   1. 仅提供类型变量（约束列表为空）
 *   2. 从另一个 [VariableWithConstraints] 复制
 *   3. [UnstableSystemMergeMode] 下合并两个 [VariableWithConstraints]（用于 OverloadResolutionByLambdaReturnType）
 */
class MutableVariableWithConstraints private constructor(
    /** 所属约束系统上下文。 */
    private val context: Context,
    /** 当前约束集合关联的类型变量。 */
    override val typeVariable: TypeVariableMarker,
    constraints: List<Constraint>?, // 已经过化简与去重的约束列表；null 表示初始化为空
) : VariableWithConstraints {

    /** 从单个类型变量创建，约束列表初始为空 */
    constructor(context: Context, typeVariable: TypeVariableMarker)
            : this(context, typeVariable, null)

    /** 从已有的 [VariableWithConstraints] 复制约束 */
    constructor(context: Context, other: VariableWithConstraints)
            : this(context, other.typeVariable, other.constraints)

    /**
     * 仅在 [UnstableSystemMergeMode] 下使用：将两个变量的约束集合以身份哈希去重后合并。
     * 要求两个变量的 typeVariable 相同。
     */
    @UnstableSystemMergeMode
    constructor(context: Context, first: VariableWithConstraints, second: VariableWithConstraints) : this(
        context,
        first.typeVariable.also { require(it == second.typeVariable) },
        identityHashSetFromSum(first.constraints, second.constraints).toList(),
    )

    /**
     * 对外暴露的约束列表（已化简）。
     * 首次访问时对 [mutableConstraints] 执行化简，并缓存结果到 [simplifiedConstraints]。
     */
    override val constraints: List<Constraint>
        get() {
            if (simplifiedConstraints == null) {
                simplifiedConstraints = mutableConstraints.simplifyConstraints()
            }
            return simplifiedConstraints!!
        }

    /**
     * 获取所有"仅输入类型"位置的约束类型，供 @OnlyInputTypes 注解的推断逻辑使用。
     * 返回 (类型, 约束种类) 对的集合。
     */
    fun getProjectedInputCallTypes(utilContext: ConstraintSystemUtilContext): Collection<Pair<CangJieTypeMarker, ConstraintKind>> {
        return with(utilContext) {
            mutableConstraints
                .mapNotNullTo(SmartList()) {
                    if (it.position.from is OnlyInputTypeConstraintPosition || it.inputTypePositionBeforeIncorporation != null)
                        it.type.unCapture() to it.kind
                    else null
                }
        }
    }

    /** 原始可变约束列表，所有修改操作均直接作用于此列表 */
    private val mutableConstraints = if (constraints == null) SmartList() else SmartList(constraints)

    /**
     * 化简后的约束缓存。
     *
     * 维护契约：**唯一允许的变更是追加元素**。若发生其他变更，必须将其置为 null，
     * 以便下次访问 [constraints] 时重新计算。
     *
     * 这样设计是因为列表可能在被迭代时同时被追加，
     * 参见 [ConstraintIncorporator.forEachConstraint] 中的索引循环。
     */
    private var simplifiedConstraints: SmartList<Constraint>? = mutableConstraints

    /**
     * 按"所含类型变量构造器"分组的约束缓存。
     * 仅用于性能优化，任何修改约束的操作后必须置为 null。
     */
    private var constraintsGroupedByContainedTypeVariables: Map<TypeConstructorMarker, Collection<Constraint>>? = null

    /**
     * 按约束类型哈希值分组的约束缓存。
     * 仅用于性能优化，任何修改约束的操作后必须置为 null。
     */
    private var constraintsGroupedByTypeHashCode: MutableMap<Int, MutableList<Constraint>>? = null

    /**
     * 清除两个分组缓存。
     * 所有对 [mutableConstraints] 的修改（除纯追加外）都应调用此方法。
     */
    private fun clearGroupedConstraintCaches() {
        constraintsGroupedByContainedTypeVariables = null
        constraintsGroupedByTypeHashCode = null
    }

    /**
     * 返回所有包含指定类型变量构造器的约束。
     * 利用 [constraintsGroupedByContainedTypeVariables] 缓存加速查找。
     */
    override fun getConstraintsContainedSpecifiedTypeVariable(typeVariableConstructor: TypeConstructorMarker): Collection<Constraint> {
        if (constraintsGroupedByContainedTypeVariables == null) {
            constraintsGroupedByContainedTypeVariables = computeConstraintsGroupedByContainedTypeVariables()
        }
        return constraintsGroupedByContainedTypeVariables!![typeVariableConstructor] ?: emptyList()
    }

    /** 遍历所有约束，按其中包含的类型变量构造器建立分组映射 */
    private fun computeConstraintsGroupedByContainedTypeVariables(): Map<TypeConstructorMarker, Collection<Constraint>> = with(context) {
        buildMap<TypeConstructorMarker, MutableCollection<Constraint>> {
            for (constraint in constraints) {
                for (otherTypeVariable in constraint.type.extractAllContainingTypeVariables()) {
                    this.getOrPut(otherTypeVariable) { SmartList() }.add(constraint)
                }
            }
        }
    }

    /**
     * 返回与给定约束类型哈希值相同的所有已有约束。
     * 利用 [constraintsGroupedByTypeHashCode] 缓存加速查找，避免全量遍历。
     */
    private fun getConstraintsWithSameTypeHashCode(c: Constraint): List<Constraint> {
        if (constraintsGroupedByTypeHashCode == null) {
            constraintsGroupedByTypeHashCode = constraints.groupByTo(mutableMapOf(), Constraint::typeHashCode)
        }
        return constraintsGroupedByTypeHashCode!![c.typeHashCode].orEmpty()
    }

    /** 原始约束列表中的元素数量（未经化简） */
    val rawConstraintsCount get() = mutableConstraints.size

    /**
     * 尝试添加一条约束。
     *
     * 返回值为 Pair：
     * - first：实际生效的约束（可能是已有约束，也可能是新生成的 EQUALITY 约束）
     * - second：是否真正添加了新约束（true = 已添加，false = 被已有约束覆盖而跳过）
     *
     * 核心逻辑：
     * 1. 若存在相同类型、相同标志的旧约束且新约束冗余，直接返回旧约束。
     * 2. 若旧约束与新约束方向互补（一个 LOWER 一个 UPPER），合并为 EQUALITY 约束。
     * 3. 否则追加新约束到列表。
     */
    fun addConstraint(constraint: Constraint, inferenceLogger: InferenceLogger?): Pair<Constraint, Boolean> {
        for (previousConstraint in getConstraintsWithSameTypeHashCode(constraint)) {
            if (previousConstraint.type == constraint.type
                && previousConstraint.isNoInfer == constraint.isNoInfer
            ) {
                // 新约束被旧约束完全覆盖，直接丢弃
                if (newConstraintIsUseless(previousConstraint, constraint)) {
                    return previousConstraint to false
                }

                // 判断是否满足合并为 EQUALITY 的条件（LOWER + UPPER 或已经是 EQUALITY）
                val isMatchingForSimplification = when (previousConstraint.kind) {
                    ConstraintKind.LOWER -> constraint.kind.isUpper()
                    ConstraintKind.UPPER -> constraint.kind.isLower()
                    ConstraintKind.EQUALITY -> true
                }
                if (isMatchingForSimplification) {
                    // 将互补的 LOWER/UPPER 合并为 EQUALITY 约束
                    val actualConstraint = if (constraint.kind != ConstraintKind.EQUALITY) {
                        Constraint(
                            ConstraintKind.EQUALITY,
                            constraint.type,
                            // 优先使用非 DeclaredUpperBound 位置，保留用户定义的约束来源信息
                            constraint.position.takeIf { it.from !is DeclaredUpperBoundConstraintPosition<*> }
                                ?: previousConstraint.position,
                            constraint.typeHashCode,
                            derivedFrom = constraint.derivedFrom,
                            isNoInfer = constraint.isNoInfer,
                        ).also {
                            // 记录推断日志：该 EQUALITY 约束由哪两条约束合并而来
                            inferenceLogger?.withOrigins(
                                typeVariable, previousConstraint,
                                typeVariable, constraint,
                            ) {
                                inferenceLogger?.logReadiness(InferenceLogger.FixationLogRecord(emptyMap(), typeVariable), context)
                            }
                        }
                    } else constraint
                    mutableConstraints.add(actualConstraint)
                    clearGroupedConstraintCaches()
                    simplifiedConstraints = null
                    return actualConstraint to true
                }
            }
        }

        // 没有可合并/覆盖的旧约束，直接追加
        mutableConstraints.add(constraint)
        // 若 simplifiedConstraints 是独立列表（非 mutableConstraints 本身），同步追加以保持一致
        if (simplifiedConstraints != null && simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints!!.add(constraint)
        }

        // 追加后更新类型哈希分组缓存，并清除类型变量分组缓存
        addConstraintToCacheByTypeHashCode(constraint)
        constraintsGroupedByContainedTypeVariables = null

        return constraint to true
    }

    /** 将约束追加到类型哈希分组缓存（仅在缓存已初始化时执行） */
    private fun addConstraintToCacheByTypeHashCode(constraint: Constraint) {
        constraintsGroupedByTypeHashCode?.getOrPut(constraint.typeHashCode) { mutableListOf() }?.add(constraint)
    }

    /**
     * 移除 [sinceIndex] 之后（含）的所有约束。
     * **仅供约束系统事务回滚使用**，要求被移除的元素均在列表尾部。
     */
    internal fun removeLastConstraints(sinceIndex: Int) {
        mutableConstraints.trimToSize(sinceIndex)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
        clearGroupedConstraintCaches()
    }

    /**
     * 按条件移除约束。
     * **仅在约束系统处于 COMPLETION 状态时使用**。
     */
    internal fun removeConstraints(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.removeAll(shouldRemove)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
        clearGroupedConstraintCaches()
    }

    /**
     * 判断新约束 [new] 是否被旧约束 [old] 完全覆盖（即新约束冗余可丢弃）。
     *
     * 以下情况新约束**不**冗余（返回 false）：
     * - 旧约束来自 DeclaredUpperBound，而新约束不是（用户定义约束优先级更高）
     * - 旧约束来自 ExpectedType 且新约束不是，且双方均为 UPPER（避免丢失非预期类型的上界）
     * - 旧约束带有 NoInfer 标记而新约束没有
     *
     * 其余情况按 kind 判断：EQUALITY 覆盖一切；LOWER/UPPER 覆盖同方向约束。
     */
    private fun newConstraintIsUseless(old: Constraint, new: Constraint): Boolean {
        // 用户定义约束优先于 DeclaredUpperBound 约束
        if (old.position.from is DeclaredUpperBoundConstraintPosition<*> && new.position.from !is DeclaredUpperBoundConstraintPosition<*>)
            return false

        // 避免丢失非预期类型的上界约束（防止如 String & Int 这类错误推断）
        // 参见 ResultTypeResolver.kt 中对 ExpectedType 上界的特殊处理
        if (old.position.from is ExpectedTypeConstraintPosition<*>
            && new.position.from !is ExpectedTypeConstraintPosition<*>
            && old.kind.isUpper() && new.kind.isUpper()
        ) return false

        // NoInfer 约束不应覆盖普通约束
        if (old.isNoInfer && !new.isNoInfer) return false

        return when (old.kind) {
            ConstraintKind.EQUALITY -> true          // EQUALITY 覆盖任意同类型约束
            ConstraintKind.LOWER -> new.kind.isLower() // LOWER 覆盖同方向 LOWER
            ConstraintKind.UPPER -> new.kind.isUpper() // UPPER 覆盖同方向 UPPER
        }
    }

    /**
     * 对约束列表执行完整化简：
     * 仅保留等式约束的化简（移除被等式约束冗余的非等式约束）。
     */
    private fun SmartList<Constraint>.simplifyConstraints(): SmartList<Constraint> =
        simplifyEqualityConstraints()

    /**
     * 化简等式约束：若某条非等式约束的类型已被某条等式约束覆盖，则将其移除。
     * 这样可以减少后续推断中需要处理的约束数量。
     */
    private fun SmartList<Constraint>.simplifyEqualityConstraints(): SmartList<Constraint> {
        val equalityConstraints = filter { it.kind == ConstraintKind.EQUALITY }.groupBy { it.typeHashCode }
        return when {
            equalityConstraints.isEmpty() -> this
            else -> filterTo(SmartList()) { isUsefulConstraint(it, equalityConstraints) }
        }
    }

    /**
     * 判断某条约束在化简过程中是否有保留价值：
     * - 等式约束始终保留
     * - 非等式约束：若已存在类型相同的等式约束，则认为它冗余可丢弃
     */
    private fun isUsefulConstraint(constraint: Constraint, equalityConstraints: Map<Int, List<Constraint>>): Boolean {
        if (constraint.kind == ConstraintKind.EQUALITY) return true
        return equalityConstraints[constraint.typeHashCode]?.none { it.type == constraint.type } ?: true
    }

    /**
     * 返回便于调试的约束集合描述。
     */
    override fun toString(): String {
        return "Constraints for $typeVariable"
    }
}


/** 约束系统的可变存储，持有所有类型变量及其约束、错误、固定结果等信息 */
internal class MutableConstraintStorage : ConstraintStorage {
    /** 所有类型变量（包括已固定和未固定），以类型构造器为键 */
    override val allTypeVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker> = LinkedHashMap()

    /** 尚未固定的类型变量及其约束集合 */
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints> = LinkedHashMap()

    /** 类型变量间的依赖关系（用于确定固定顺序） */
    override val typeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> =
        LinkedHashMap()

    /** 初始约束列表（用户显式提供或调用点直接产生的约束） */
    override val initialConstraints: MutableList<InitialConstraint> = SmartList()

    /** 初始约束中类型的最大嵌套深度，用于控制类型近似的精度 */
    override var maxTypeDepthFromInitialConstraints: Int = 1

    /** 约束系统收集到的错误列表 */
    override val errors: MutableList<ConstraintSystemError> = SmartList()

    /** 是否存在矛盾（即存在适用性不成功的错误） */
    @OptIn(ApplicabilityDetail::class)
    override val hasContradiction: Boolean get() = errors.any { !it.applicability.isSuccess }

    /** 已固定的类型变量及其推断出的具体类型 */
    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker> = LinkedHashMap()

    /** 延迟处理的类型变量列表（如 lambda 参数类型等需要二阶段推断的变量） */
    override val postponedTypeVariables: MutableList<TypeVariableMarker> = SmartList()

    /**
     * 为延迟参数（postponed argument）按顶层类型变量构建的函数类型缓存。
     * Key：(顶层类型变量构造器, 参数位置列表) 对
     */
    override val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: MutableMap<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker> =
        LinkedHashMap()

    /**
     * 为延迟参数按预期类型变量构建的函数类型缓存。
     * Key：预期类型变量构造器
     */
    override val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker> =
        LinkedHashMap()

    /** 所有分叉点（fork point）产生的约束，用于多候选重载分析 */
    override val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>> = SmartList()

    /** 外部约束系统变量的前缀大小（用于嵌套约束系统场景） */
    override var outerSystemVariablesPrefixSize: Int = 0

    /** 是否使用了外部约束系统（嵌套推断场景） */
    override var usesOuterCs: Boolean = false

    /** 类型近似器的配置级别缓存 */
    override val approximatorCaches: TypeApproximatorCachesPerConfiguration = mutableMapOf()

    /** 仅用于断言/调试：指向外部约束系统的引用，不影响语义 */
    @AssertionsOnly
    internal var outerCS: ConstraintStorage? = null
}

/**
 * 将两个列表中的元素合并到一个基于身份（引用）去重的集合中。
 * 使用 [IdentityHashMap] 确保以对象引用而非 equals() 判断重复。
 */
fun <T> identityHashSetFromSum(first: List<T>, second: List<T>): Set<T> =
    IdentityHashMap<T, Boolean>().apply {
        for (elem in first) put(elem, true)
        for (elem in second) put(elem, true)
    }.keys

/**
 * 标记该成员仅用于断言目的，不影响任何语义行为。
 * 使用时需显式 opt-in。
 */
@RequiresOptIn
annotation class AssertionsOnly

/**
 * 标记该成员仅在"约束系统合并"的不稳定模式下使用，
 * 专用于 OverloadResolutionByLambdaReturnType 的解析流程。
 * 请勿在其他模式中使用。
 */
@RequiresOptIn(
    message = "This member is a part of unstable constraint system merge mode and " +
            "is intended to be used exclusively for OverloadResolutionByLambdaReturnType resolve. " +
            "Please don't use in other modes."
)
/**
 * 标记约束系统不稳定合并模式专用 API。
 */
annotation class UnstableSystemMergeMode
