package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 类型推断约束。
 *
 * 约束系统中的单个约束，描述类型变量与具体类型之间的关系。
 *
 * 对齐 K2 Constraint（简化为 2 种，去掉 TypeInEquality 等）。
 */
sealed class CfirConstraint {
    /** 约束来源位置 */
    abstract val position: CfirConstraintPosition
}

/**
 * 子类型约束：[subType] <: [superType]。
 *
 * 表示 subType 必须是 superType 的子类型。
 * 若 subType 或 superType 中包含类型变量，约束系统将据此收集上界/下界。
 */
class CfirSubtypeConstraint(
    val subType: ConeCangjieType,
    val superType: ConeCangjieType,
    override val position: CfirConstraintPosition,
) : CfirConstraint() {
    override fun toString(): String = "$subType <: $superType @ $position"
}

/**
 * 等价约束：[left] == [right]。
 *
 * 表示两个类型必须完全相等。
 * 通常来自类型参数出现在不变位置的场景（如数组元素类型）。
 */
class CfirEqualityConstraint(
    val left: ConeCangjieType,
    val right: ConeCangjieType,
    override val position: CfirConstraintPosition,
) : CfirConstraint() {
    override fun toString(): String = "$left == $right @ $position"
}
