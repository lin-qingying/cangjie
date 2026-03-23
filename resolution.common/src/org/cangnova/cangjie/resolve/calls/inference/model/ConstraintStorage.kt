/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.TypeApproximatorCachesPerConfiguration

/**
 * 约束存储接口，描述类型变量推断系统的完整状态。
 *
 * 每个类型变量处于以下两种状态之一：
 *
 * **未固定（not fixed）**：存在零条或多条约束，该变量在 [notFixedTypeVariables] 中有对应的
 * [VariableWithConstraints] 记录。
 *
 * **已固定（fixed）**：已推断出具体类型（可以是 proper 或 non-proper 类型），
 * 该变量从 [notFixedTypeVariables] 中移除，结果存入 [fixedTypeVariables]。
 * 系统保证其他变量的约束中不再出现对该变量的引用。
 *
 * **固定流程（针对 proper 类型）**：
 * 1. 确定固定顺序（依赖关系拓扑排序）
 * 2. 对每个类型变量：确定结果类型 → 添加等式约束（如 T = Int）→ 执行 incorporation 生成新约束
 *    → 从 [notFixedTypeVariables] 移除 → 清理其他变量中引用该变量的约束 → 写入 [fixedTypeVariables]
 *
 * 固定为 non-proper 类型的流程相同，区别仅在于结果类型的确定方式。
 */
interface ConstraintStorage {
    /** 所有类型变量（含已固定和未固定），以类型构造器为键 */
    val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

    /** 尚未固定的类型变量及其约束集合 */
    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    /** 调用点直接产生的初始约束列表 */
    val initialConstraints: List<InitialConstraint>

    /** 初始约束中类型的最大嵌套深度，用于控制类型近似的精度 */
    val maxTypeDepthFromInitialConstraints: Int

    /** 推断过程中收集到的错误列表 */
    val errors: List<ConstraintSystemError>

    /** 是否存在矛盾（即存在导致推断失败的错误） */
    val hasContradiction: Boolean

    /** 已固定的类型变量及其推断出的具体类型 */
    val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>

    /** 延迟处理的类型变量列表（如 lambda 参数等需要二阶段推断的变量） */
    val postponedTypeVariables: List<TypeVariableMarker>

    /**
     * 为延迟参数按顶层类型变量构建的函数类型缓存。
     * Key：(顶层类型变量构造器, 参数位置列表) 对
     */
    val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: Map<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker>

    /**
     * 为延迟参数按预期类型变量构建的函数类型缓存。
     * Key：预期类型变量构造器
     */
    val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>

    /** 所有分叉点（fork point）产生的约束，用于多候选重载分析 */
    val constraintsFromAllForkPoints: List<Pair<IncorporationConstraintPosition, ForkPointData>>

    /**
     * 类型变量间的依赖关系映射：对于类型变量 X（以其类型构造器为键），
     * 值为所有**可能**在约束中引用了 X 的其他类型变量的构造器集合。
     *
     * 主要用于 incorporation 阶段的性能优化，避免全量扫描。
     *
     * 注意：该集合可能包含误报（false positive）——当某条约束在事务回滚中被移除后，
     * 对应的依赖关系不会被同步清理，因此可能残留不再实际引用 X 的变量。
     */
    val typeVariableDependencies: Map<TypeConstructorMarker, Set<TypeConstructorMarker>>

    /** 类型近似器的配置级别缓存，避免重复计算 */
    val approximatorCaches: TypeApproximatorCachesPerConfiguration

    /**
     * 外部约束系统变量的前缀大小。
     *
     * 当某个候选的约束系统在外部约束系统的上下文中构建时，[allTypeVariables] 列表中
     * 前 [outerSystemVariablesPrefixSize] 个变量属于外部约束系统。
     *
     * 该信息仅在以下两处有限使用：
     * - 完成 `provideDelegate` 调用时，将外部变量视为 proper 类型
     *   （参见 fixInnerVariablesForProvideDelegateIfNeeded）
     * - 校验内部候选收集的变量数量一致性
     *   （参见 checkNotFixedTypeVariablesCountConsistency）
     *
     * 另见 docs/fir/delegated_property_inference.md
     */
    val outerSystemVariablesPrefixSize: Int

    /** 是否使用了外部约束系统（嵌套推断场景） */
    val usesOuterCs: Boolean

    /** 空约束存储单例，所有属性均返回空集合/默认值，用于无约束的初始状态 */
    object Empty : ConstraintStorage {
        override val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker> get() = emptyMap()
        override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints> get() = emptyMap()
        override val initialConstraints: List<InitialConstraint> get() = emptyList()
        override val maxTypeDepthFromInitialConstraints: Int get() = 1
        override val errors: List<ConstraintSystemError> get() = emptyList()
        override val hasContradiction: Boolean get() = false
        override val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker> get() = emptyMap()
        override val postponedTypeVariables: List<TypeVariableMarker> get() = emptyList()
        override val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: Map<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker> = emptyMap()
        override val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker> = emptyMap()
        override val constraintsFromAllForkPoints: List<Pair<IncorporationConstraintPosition, ForkPointData>> = emptyList()
        override val typeVariableDependencies: Map<TypeConstructorMarker, Set<TypeConstructorMarker>> get() = emptyMap()
        override val outerSystemVariablesPrefixSize: Int get() = 0
        override val usesOuterCs: Boolean get() = false
        override val approximatorCaches: TypeApproximatorCachesPerConfiguration get() = mutableMapOf()
    }
}

/**
 * 约束的方向/种类。
 *
 * - [LOWER]：下界约束，表示"类型变量 T 的实际类型 >= 该类型"（该类型是 T 的子类型）
 * - [UPPER]：上界约束，表示"类型变量 T 的实际类型 <= 该类型"（该类型是 T 的超类型）
 * - [EQUALITY]：等式约束，表示"类型变量 T 的实际类型 == 该类型"（同时隐含上下界）
 */
enum class ConstraintKind {
    LOWER,
    UPPER,
    EQUALITY;

    /** 是否为下界约束 */
    fun isLower(): Boolean = this == LOWER

    /** 是否为上界约束 */
    fun isUpper(): Boolean = this == UPPER

    /** 是否为等式约束 */
    fun isEqual(): Boolean = this == EQUALITY

    /**
     * 是否隐含下界语义。
     * LOWER 和 EQUALITY 均隐含下界（EQUALITY 同时隐含上下界）。
     */
    fun impliesLower(): Boolean = !isUpper()

    /** 返回方向相反的约束种类（LOWER ↔ UPPER，EQUALITY 保持不变） */
    fun opposite() = when (this) {
        LOWER -> UPPER
        UPPER -> LOWER
        EQUALITY -> EQUALITY
    }
}

/**
 * 单条约束，表示类型变量与某个具体类型之间的关系。
 *
 * @param kind 约束方向：LOWER / UPPER / EQUALITY
 * @param type 约束涉及的类型
 * @param position 约束的来源位置（用于错误报告和推断日志）
 * @param typeHashCode [type] 的哈希值，用于快速查找和去重
 * @param derivedFrom 通过 incorporation 传播时，所有参与推导的原始类型变量集合。
 *   例如 `α <: Number, β <: Inv<α>` 推导出 `β <: Inv<out Number>` 时，
 *   新约束的 derivedFrom 是两条原始约束 derivedFrom 的并集。
 *   该字段用于防止 incorporation 无限递归。
 * @param isNoInfer 标记该约束携带 `@NoInfer` 语义，不参与常规推断。
 * @param inputTypePositionBeforeIncorporation incorporation 前的 OnlyInputType 位置，
 *   用于 @OnlyInputTypes 注解的推断逻辑。
 */
class Constraint(
    val kind: ConstraintKind,
    val type: CangJieTypeMarker,
    val position: IncorporationConstraintPosition,
    val typeHashCode: Int = type.hashCode(),
    val derivedFrom: Set<TypeVariableMarker>,
    val isNoInfer: Boolean,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Constraint

        if (typeHashCode != other.typeHashCode) return false
        if (kind != other.kind) return false
        if (position != other.position) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode() = typeHashCode

    override fun toString() = "$kind($type) from $position"
}

/**
 * 持有某个类型变量及其所有约束的只读视图。
 */
interface VariableWithConstraints {
    /** 对应的类型变量 */
    val typeVariable: TypeVariableMarker

    /** 当前有效的约束列表（已化简） */
    val constraints: List<Constraint>

    /**
     * 返回所有包含指定类型变量构造器的约束。
     * 仅用于 incorporation 阶段的性能优化。
     */
    fun getConstraintsContainedSpecifiedTypeVariable(typeVariableConstructor: TypeConstructorMarker): Collection<Constraint>
}

/**
 * 调用点直接产生的初始约束，记录约束两端的类型及其来源位置。
 *
 * [constraintKind] 的语义：
 * - LOWER：a 是 b 的子类型（a <: b）
 * - UPPER：b 是 a 的子类型（b <: a）
 * - EQUALITY：a 与 b 类型相同
 */
class InitialConstraint(
    val a: CangJieTypeMarker,
    val b: CangJieTypeMarker,
    val constraintKind: ConstraintKind,
    val position: ConstraintPosition
) {
    override fun toString(): String = "${asStringWithoutPosition()} from $position"

    /** 返回不含位置信息的约束字符串表示，便于调试输出 */
    fun asStringWithoutPosition(): String {
        val sign = when (constraintKind) {
            ConstraintKind.EQUALITY -> "=="
            ConstraintKind.LOWER    -> ":>"
            ConstraintKind.UPPER    -> "<:"
        }
        return "$a $sign $b"
    }
}

/**
 * 校验某个约束在给定结果类型下是否成立。
 *
 * @param constraintType 约束中的类型（如上界或下界类型）
 * @param constraintKind 约束方向
 * @param resultType 类型变量推断出的结果类型
 * @return 约束是否被满足
 */
context(context: TypeCheckerProviderContext)
fun checkConstraint(
    constraintType: CangJieTypeMarker,
    constraintKind: ConstraintKind,
    resultType: CangJieTypeMarker
): Boolean {
    val typeChecker = AbstractTypeChecker
    return when (constraintKind) {
        // 等式约束：结果类型与约束类型必须完全相等
        ConstraintKind.EQUALITY -> typeChecker.equalTypes(context, constraintType, resultType)
        // 下界约束：constraintType <: resultType（约束类型是结果类型的子类型）
        ConstraintKind.LOWER -> typeChecker.isSubtypeOf(context, constraintType, resultType)
        // 上界约束：resultType <: constraintType（结果类型是约束类型的子类型）
        ConstraintKind.UPPER -> typeChecker.isSubtypeOf(context, resultType, constraintType)
    }
}