package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 类型推断中的约束。
 * 约束系统里的每条约束都描述了类型变量与具体类型之间的关系。
 * 对齐 K2 `Constraint`，但这里只保留两类基础约束。
 */
sealed class CfirConstraint {
    /** 约束来源位置。 */
    abstract val position: CfirConstraintPosition
}

/**
 * 子类型约束：`subType <: superType`。
 * 若两端包含类型变量，约束系统会据此收集上下界信息。
 */
class CfirSubtypeConstraint(
    val subType: ConeCangjieType,
    val superType: ConeCangjieType,
    override val position: CfirConstraintPosition,
) : CfirConstraint() {
    override fun toString(): String = "$subType <: $superType @ $position"
}

/**
 * 等价约束：`left == right`。
 * 一般用于要求两个类型完全相等的场景。
 */
class CfirEqualityConstraint(
    val left: ConeCangjieType,
    val right: ConeCangjieType,
    override val position: CfirConstraintPosition,
) : CfirConstraint() {
    override fun toString(): String = "$left == $right @ $position"
}

